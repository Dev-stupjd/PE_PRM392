package com.koigzzzz.cex.utils;

import android.util.Log;

import com.koigzzzz.cex.api.LiveCoinWatchService;
import com.koigzzzz.cex.models.LiveCoinWatchResponse;
import com.koigzzzz.cex.models.TokenPrice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class PriceManager {
    private static final String TAG = "PriceManager";
    private static final String BASE_URL = "https://api.livecoinwatch.com/";
    private static final int TIMEOUT_SECONDS = 15;
    private static PriceManager instance;
    private LiveCoinWatchService apiService;
    private Map<String, TokenPrice> priceCache;
    private Map<String, Long> cacheTimestamps; // Track when each price was cached
    private String apiKey;
    
    // Cache duration: 5 minutes (300,000 ms) - prices are acceptable if less than 5 min old
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;
    
    // Rate limit tracking: 10,000 requests per day = ~416 per hour = ~7 per minute
    private AtomicLong lastRequestTime = new AtomicLong(0);
    private static final long MIN_REQUEST_INTERVAL_MS = 9000; // Minimum 9 seconds between requests

    // LiveCoinWatch uses coin codes directly
    private static final String[] SUPPORTED_TOKENS = {"BTC", "ETH", "SOL", "BNB"};

    private PriceManager() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(LiveCoinWatchService.class);
        priceCache = new HashMap<>();
        cacheTimestamps = new HashMap<>();
        
        // Note: API key should be set from app context
        // For now, using placeholder - should be set via setApiKey() method
        apiKey = "YOUR_API_KEY_HERE";
    }

    public static synchronized PriceManager getInstance() {
        if (instance == null) {
            instance = new PriceManager();
        }
        return instance;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public interface PriceCallback {
        void onPriceReceived(TokenPrice tokenPrice);
        void onError(String error);
    }

    public void fetchPrice(String symbol, PriceCallback callback) {
        // Removed isSupported check - tokens are now dynamic from Firestore
        // API will handle unsupported tokens by returning an error response
        
        String symbolUpper = symbol.toUpperCase();
        
        // Check cache first - return cached price if still fresh
        TokenPrice cachedPrice = priceCache.get(symbolUpper);
        Long cacheTime = cacheTimestamps.get(symbolUpper);
        
        if (cachedPrice != null && cacheTime != null) {
            long age = System.currentTimeMillis() - cacheTime;
            if (age < CACHE_DURATION_MS) {
                // Cache is still fresh, return it immediately
                callback.onPriceReceived(cachedPrice);
                return;
            }
        }

        // Rate limiting: ensure minimum time between requests
        long now = System.currentTimeMillis();
        long lastRequest = lastRequestTime.get();
        long timeSinceLastRequest = now - lastRequest;
        
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            // Return cached price even if stale, rather than hitting rate limit
            if (cachedPrice != null) {
                callback.onPriceReceived(cachedPrice);
                return;
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("currency", "USD");
        requestBody.put("code", symbolUpper);
        requestBody.put("meta", false);

        lastRequestTime.set(now);
        Call<LiveCoinWatchResponse> call = apiService.getCoinPrice(apiKey, requestBody);

        call.enqueue(new Callback<LiveCoinWatchResponse>() {
            @Override
            public void onResponse(Call<LiveCoinWatchResponse> call,
                                   Response<LiveCoinWatchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LiveCoinWatchResponse lcwResponse = response.body();
                    
                    double price = lcwResponse.getRate();
                    double change24h = 0.0;
                    if (lcwResponse.getDelta() != null && lcwResponse.getDelta().getDay() != 0) {
                        // delta.day is a multiplier (e.g., 1.0808 = +8.08%), convert to percentage
                        change24h = (lcwResponse.getDelta().getDay() - 1.0) * 100.0;
                    }
                    double volume24h = lcwResponse.getVolume();

                    TokenPrice tokenPrice = new TokenPrice(
                            symbol.toUpperCase(),
                            lcwResponse.getName() != null ? lcwResponse.getName() : getTokenName(symbol),
                            price,
                            change24h,
                            volume24h
                    );

                    priceCache.put(symbolUpper, tokenPrice);
                    cacheTimestamps.put(symbolUpper, System.currentTimeMillis());
                    
                    // Track price history for charts
                    PriceHistoryTracker.getInstance().addPricePoint(symbolUpper, price);
                    
                    callback.onPriceReceived(tokenPrice);
                    } else {
                        String errorMsg = response.message();
                        if (response.code() == 401) {
                            errorMsg = "Invalid API key. Please check your LiveCoinWatch API key.";
                        } else if (response.code() == 429) {
                            errorMsg = "Rate limit exceeded. Using cached data if available.";
                            // On rate limit, return cached price even if stale
                            if (cachedPrice != null) {
                                callback.onPriceReceived(cachedPrice);
                                return;
                            }
                            // Extend wait time after rate limit error
                            lastRequestTime.set(System.currentTimeMillis() + (60 * 1000)); // Wait 1 minute
                        }
                        Log.e(TAG, "API error: " + response.code() + " - " + errorMsg);
                        // Return cached price on error if available
                        if (cachedPrice != null) {
                            callback.onPriceReceived(cachedPrice);
                        } else {
                            callback.onError("API error: " + errorMsg);
                        }
                    }
            }

            @Override
            public void onFailure(Call<LiveCoinWatchResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching price for " + symbol, t);
                // Return cached price on network error if available
                if (cachedPrice != null) {
                    callback.onPriceReceived(cachedPrice);
                } else {
                    callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
                }
            }
        });
    }

    public void fetchAllPrices(AllPricesCallback callback) {
        // Use default supported tokens
        fetchPricesForSymbols(SUPPORTED_TOKENS, callback);
    }

    /**
     * Fetch prices for a list of token symbols dynamically
     */
    public void fetchPricesForSymbols(String[] symbols, AllPricesCallback callback) {
        Map<String, TokenPrice> tokenPrices = new HashMap<>();
        
        // First, check cache and add any fresh cached prices
        boolean allCachedFresh = true;
        for (String symbol : symbols) {
            String symbolUpper = symbol.toUpperCase();
            TokenPrice cachedPrice = priceCache.get(symbolUpper);
            Long cacheTime = cacheTimestamps.get(symbolUpper);
            
            if (cachedPrice != null && cacheTime != null) {
                long age = System.currentTimeMillis() - cacheTime;
                if (age < CACHE_DURATION_MS) {
                    tokenPrices.put(symbolUpper, cachedPrice);
                    continue;
                } else {
                    allCachedFresh = false;
                }
            } else {
                allCachedFresh = false;
            }
        }
        
        // If all prices are fresh in cache, return immediately
        if (allCachedFresh && tokenPrices.size() == symbols.length) {
            callback.onPricesReceived(tokenPrices);
            return;
        }
        
        // Rate limiting: check if we can make requests
        long now = System.currentTimeMillis();
        long lastRequest = lastRequestTime.get();
        long timeSinceLastRequest = now - lastRequest;
        
        // If we can't make requests yet, return cached data (even if stale)
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            if (!tokenPrices.isEmpty()) {
                callback.onPricesReceived(tokenPrices);
            } else {
                // No cache available, return error
                callback.onError("Rate limited. Please wait before refreshing.");
            }
            return;
        }

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        
        // Remove symbols that are already in cache from fetch list
        String[] tokensToFetch = tokenPrices.size() < symbols.length ?
            java.util.Arrays.stream(symbols)
                .filter(s -> !tokenPrices.containsKey(s.toUpperCase()))
                .toArray(String[]::new) : symbols;

        if (tokensToFetch.length == 0) {
            callback.onPricesReceived(tokenPrices);
            return;
        }

        String[] errors = new String[tokensToFetch.length];
        lastRequestTime.set(now);

        for (int i = 0; i < tokensToFetch.length; i++) {
            final String symbol = tokensToFetch[i];
            final int index = i;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("currency", "USD");
            requestBody.put("code", symbol);
            requestBody.put("meta", false);

            Call<LiveCoinWatchResponse> call = apiService.getCoinPrice(apiKey, requestBody);

            call.enqueue(new Callback<LiveCoinWatchResponse>() {
                @Override
                public void onResponse(Call<LiveCoinWatchResponse> call,
                                       Response<LiveCoinWatchResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LiveCoinWatchResponse lcwResponse = response.body();
                        
                        double price = lcwResponse.getRate();
                        double change24h = 0.0;
                        if (lcwResponse.getDelta() != null && lcwResponse.getDelta().getDay() != 0) {
                            // delta.day is a multiplier (e.g., 1.0808 = +8.08%), convert to percentage
                            change24h = (lcwResponse.getDelta().getDay() - 1.0) * 100.0;
                        }
                        double volume24h = lcwResponse.getVolume();

                        TokenPrice tokenPrice = new TokenPrice(
                                symbol,
                                lcwResponse.getName() != null ? lcwResponse.getName() : getTokenName(symbol),
                                price,
                                change24h,
                                volume24h
                        );

                        tokenPrices.put(symbol, tokenPrice);
                        priceCache.put(symbol, tokenPrice);
                        cacheTimestamps.put(symbol, System.currentTimeMillis());
                        
                        // Track price history for charts
                        PriceHistoryTracker.getInstance().addPricePoint(symbol, price);
                    } else {
                        String errorMsg = response.message();
                        if (response.code() == 401) {
                            errorMsg = "Invalid API key";
                        } else if (response.code() == 429) {
                            errorMsg = "Rate limit exceeded";
                            // Extend wait time after rate limit error
                            lastRequestTime.set(System.currentTimeMillis() + (60 * 1000)); // Wait 1 minute
                        }
                        errors[index] = errorMsg;
                        failed.incrementAndGet();
                    }

                    int done = completed.incrementAndGet();
                    if (done == tokensToFetch.length) {
                        // Merge with any cached prices we already had
                        for (String cachedSymbol : symbols) {
                            String cachedSymbolUpper = cachedSymbol.toUpperCase();
                            TokenPrice cachedPrice = priceCache.get(cachedSymbolUpper);
                            Long cacheTime = cacheTimestamps.get(cachedSymbolUpper);
                            if (cachedPrice != null && cacheTime != null) {
                                long age = System.currentTimeMillis() - cacheTime;
                                if (age < CACHE_DURATION_MS && !tokenPrices.containsKey(cachedSymbolUpper)) {
                                    tokenPrices.put(cachedSymbolUpper, cachedPrice);
                                }
                            }
                        }
                        
                        if (!tokenPrices.isEmpty()) {
                            callback.onPricesReceived(tokenPrices);
                        } else {
                            StringBuilder errorBuilder = new StringBuilder("Failed to fetch prices: ");
                            for (String error : errors) {
                                if (error != null) {
                                    errorBuilder.append(error).append("; ");
                                }
                            }
                            callback.onError(errorBuilder.toString());
                        }
                    }
                }

                @Override
                public void onFailure(Call<LiveCoinWatchResponse> call, Throwable t) {
                    Log.e(TAG, "Error fetching price for " + symbol, t);
                    errors[index] = t.getMessage() != null ? t.getMessage() : "Network error";
                    failed.incrementAndGet();
                    
                    int done = completed.incrementAndGet();
                    if (done == tokensToFetch.length) {
                        // Merge with any cached prices we already had
                        for (String cachedSymbol : symbols) {
                            String cachedSymbolUpper = cachedSymbol.toUpperCase();
                            TokenPrice cachedPrice = priceCache.get(cachedSymbolUpper);
                            Long cacheTime = cacheTimestamps.get(cachedSymbolUpper);
                            if (cachedPrice != null && cacheTime != null) {
                                long age = System.currentTimeMillis() - cacheTime;
                                if (age < CACHE_DURATION_MS && !tokenPrices.containsKey(cachedSymbolUpper)) {
                                    tokenPrices.put(cachedSymbolUpper, cachedPrice);
                                }
                            }
                        }
                        
                        if (!tokenPrices.isEmpty()) {
                            callback.onPricesReceived(tokenPrices);
                        } else {
                            StringBuilder errorBuilder = new StringBuilder("Failed to fetch prices: ");
                            for (String error : errors) {
                                if (error != null) {
                                    errorBuilder.append(error).append("; ");
                                }
                            }
                            callback.onError(errorBuilder.toString());
                        }
                    }
                }
            });
        }
    }

    public interface AllPricesCallback {
        void onPricesReceived(Map<String, TokenPrice> prices);
        void onError(String error);
    }

    private boolean isSupported(String symbol) {
        for (String token : SUPPORTED_TOKENS) {
            if (token.equalsIgnoreCase(symbol)) {
                return true;
            }
        }
        return false;
    }

    private String getTokenName(String symbol) {
        switch (symbol.toUpperCase()) {
            case "BTC": return "Bitcoin";
            case "ETH": return "Ethereum";
            case "SOL": return "Solana";
            case "BNB": return "BNB";
            default: return symbol;
        }
    }

    public TokenPrice getCachedPrice(String symbol) {
        return priceCache.get(symbol.toUpperCase());
    }
}

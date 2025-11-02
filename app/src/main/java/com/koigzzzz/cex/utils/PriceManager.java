package com.koigzzzz.cex.utils;

import android.util.Log;

import com.koigzzzz.cex.api.CoinGeckoService;
import com.koigzzzz.cex.models.TokenPrice;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class PriceManager {
    private static final String TAG = "PriceManager";
    private static final String BASE_URL = "https://api.coingecko.com/";
    private static final int TIMEOUT_SECONDS = 10;
    private static PriceManager instance;
    private CoinGeckoService apiService;
    private Map<String, TokenPrice> priceCache;

    // CoinGecko IDs
    private static final Map<String, String> TOKEN_IDS = new HashMap<String, String>() {{
        put("BTC", "bitcoin");
        put("ETH", "ethereum");
        put("SOL", "solana");
        put("BNB", "binancecoin");
    }};

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
        apiService = retrofit.create(CoinGeckoService.class);
        priceCache = new HashMap<>();
    }

    public static synchronized PriceManager getInstance() {
        if (instance == null) {
            instance = new PriceManager();
        }
        return instance;
    }

    public interface PriceCallback {
        void onPriceReceived(TokenPrice tokenPrice);
        void onError(String error);
    }

    public void fetchPrice(String symbol, PriceCallback callback) {
        String tokenId = TOKEN_IDS.get(symbol.toUpperCase());
        if (tokenId == null) {
            callback.onError("Unsupported token: " + symbol);
            return;
        }

        // Build IDs string for multiple tokens
        String ids = String.join(",", TOKEN_IDS.values());

        Call<Map<String, Map<String, Double>>> call = apiService.getPrices(
                ids, "usd", true, true
        );

        call.enqueue(new Callback<Map<String, Map<String, Double>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, Double>>> call,
                                   Response<Map<String, Map<String, Double>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Map<String, Double>> prices = response.body();
                    Map<String, Double> tokenData = prices.get(tokenId);

                    if (tokenData != null) {
                        double price = tokenData.getOrDefault("usd", 0.0);
                        double change24h = tokenData.getOrDefault("usd_24h_change", 0.0);
                        double volume24h = tokenData.getOrDefault("usd_24h_vol", 0.0);

                        TokenPrice tokenPrice = new TokenPrice(
                                symbol,
                                getTokenName(symbol),
                                price,
                                change24h,
                                volume24h
                        );

                        priceCache.put(symbol, tokenPrice);
                        callback.onPriceReceived(tokenPrice);
                    } else {
                        callback.onError("Price data not found for " + symbol);
                    }
                } else {
                    callback.onError("API error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, Double>>> call, Throwable t) {
                Log.e(TAG, "Error fetching price", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public void fetchAllPrices(AllPricesCallback callback) {
        String ids = String.join(",", TOKEN_IDS.values());

        Call<Map<String, Map<String, Double>>> call = apiService.getPrices(
                ids, "usd", true, true
        );

        call.enqueue(new Callback<Map<String, Map<String, Double>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, Double>>> call,
                                   Response<Map<String, Map<String, Double>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Map<String, Double>> prices = response.body();
                    Map<String, TokenPrice> tokenPrices = new HashMap<>();

                    for (Map.Entry<String, String> entry : TOKEN_IDS.entrySet()) {
                        String symbol = entry.getKey();
                        String tokenId = entry.getValue();
                        Map<String, Double> tokenData = prices.get(tokenId);

                        if (tokenData != null) {
                            double price = tokenData.getOrDefault("usd", 0.0);
                            double change24h = tokenData.getOrDefault("usd_24h_change", 0.0);
                            double volume24h = tokenData.getOrDefault("usd_24h_vol", 0.0);

                            TokenPrice tokenPrice = new TokenPrice(
                                    symbol,
                                    getTokenName(symbol),
                                    price,
                                    change24h,
                                    volume24h
                            );

                            tokenPrices.put(symbol, tokenPrice);
                            priceCache.put(symbol, tokenPrice);
                        }
                    }

                    callback.onPricesReceived(tokenPrices);
                } else {
                    callback.onError("API error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, Double>>> call, Throwable t) {
                Log.e(TAG, "Error fetching prices", t);
                callback.onError(t.getMessage());
            }
        });
    }

    public interface AllPricesCallback {
        void onPricesReceived(Map<String, TokenPrice> prices);
        void onError(String error);
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


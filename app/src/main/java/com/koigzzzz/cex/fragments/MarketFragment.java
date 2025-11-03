package com.koigzzzz.cex.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.adapters.TokenAdapter;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PriceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketFragment extends Fragment {

    private RecyclerView recyclerView;
    private TokenAdapter adapter;
    private List<TokenPrice> tokenList;
    private PriceManager priceManager;
    private FirebaseHelper firebaseHelper;
    private Handler priceRefreshHandler;
    private Runnable priceRefreshRunnable;
    private List<String> enabledTokenSymbols;
    private Map<String, String> enabledTokenNames; // Store token names from Firestore
    private static final long REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes (to respect 10k/day limit)

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceManager = PriceManager.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        enabledTokenSymbols = new ArrayList<>();
        enabledTokenNames = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market, container, false);
        
        tokenList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerViewTokens);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TokenAdapter(tokenList, tokenPrice -> {
            // Navigate to TradeFragment when token is clicked
            if (getActivity() instanceof com.koigzzzz.cex.HomeActivity) {
                ((com.koigzzzz.cex.HomeActivity) getActivity()).navigateToTrade(tokenPrice.getSymbol());
            }
        });
        recyclerView.setAdapter(adapter);

        loadEnabledTokens();

        return view;
    }

    private void startAutoRefresh() {
        priceRefreshHandler = new Handler(Looper.getMainLooper());
        priceRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && getContext() != null) {
                    loadTokenPrices();
                    priceRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        };
        priceRefreshHandler.postDelayed(priceRefreshRunnable, REFRESH_INTERVAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void stopAutoRefresh() {
        if (priceRefreshHandler != null && priceRefreshRunnable != null) {
            priceRefreshHandler.removeCallbacks(priceRefreshRunnable);
        }
    }

    private void loadEnabledTokens() {
        if (getContext() == null) return;

        firebaseHelper.getEnabledTokens(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                enabledTokenSymbols.clear();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String symbol = doc.getString("symbol");
                    String name = doc.getString("name");
                    if (symbol != null) {
                        enabledTokenSymbols.add(symbol);
                        enabledTokenNames.put(symbol.toUpperCase(), name != null ? name : symbol);
                    }
                }
                
                // If no tokens found in Firestore, use default tokens
                if (enabledTokenSymbols.isEmpty()) {
                    enabledTokenSymbols.add("BTC");
                    enabledTokenSymbols.add("ETH");
                    enabledTokenSymbols.add("SOL");
                    enabledTokenSymbols.add("BNB");
                }
                
                loadTokenPrices();
                startAutoRefresh();
            } else {
                // Fallback to default tokens if error
                enabledTokenSymbols.clear();
                enabledTokenSymbols.add("BTC");
                enabledTokenSymbols.add("ETH");
                enabledTokenSymbols.add("SOL");
                enabledTokenSymbols.add("BNB");
                loadTokenPrices();
                startAutoRefresh();
            }
        });
    }

    private void loadTokenPrices() {
        if (getContext() == null || enabledTokenSymbols.isEmpty()) return;

        // Use cached prices first for immediate display
        Map<String, TokenPrice> cached = new HashMap<>();
        for (String symbol : enabledTokenSymbols) {
            TokenPrice cachedPrice = priceManager.getCachedPrice(symbol);
            if (cachedPrice != null) {
                cached.put(symbol, cachedPrice);
            }
        }
        
        if (!cached.isEmpty()) {
            updateTokenList(cached);
        }
        
        // Fetch fresh prices for enabled tokens (dynamic list from Firestore)
        String[] symbolsArray = enabledTokenSymbols.toArray(new String[0]);
        priceManager.fetchPricesForSymbols(symbolsArray, new PriceManager.AllPricesCallback() {
            @Override
            public void onPricesReceived(Map<String, TokenPrice> prices) {
                if (isAdded() && getContext() != null) {
                    updateTokenList(prices);
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getContext() != null) {
                    // Don't show error toast, just update with whatever prices we have
                    // Some tokens might not have price data (like PI if API doesn't support it)
                    updateTokenList(new HashMap<>());
                }
            }
        });
    }
    
    private void updateTokenList(Map<String, TokenPrice> prices) {
        if (!isAdded() || getContext() == null) return;
        
        tokenList.clear();
        // Add tokens in order from enabledTokenSymbols
        // Show all enabled tokens, even if price data is not available
        for (String symbol : enabledTokenSymbols) {
            String symbolUpper = symbol.toUpperCase();
            TokenPrice tokenPrice = prices.get(symbolUpper);
            
            if (tokenPrice != null) {
                // Token has price data
                tokenList.add(tokenPrice);
            } else {
                // Token doesn't have price data yet, create placeholder
                // This ensures newly added tokens appear immediately
                String tokenName = enabledTokenNames.get(symbolUpper);
                if (tokenName == null) {
                    tokenName = symbol;
                }
                TokenPrice placeholderPrice = new TokenPrice(
                    symbolUpper,
                    tokenName,
                    0.0, // Price unavailable
                    0.0, // 24h change unavailable
                    0.0  // 24h volume unavailable
                );
                tokenList.add(placeholderPrice);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEnabledTokens();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();
    }
}


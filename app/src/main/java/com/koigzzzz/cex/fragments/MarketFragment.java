package com.koigzzzz.cex.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Button;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.adapters.TokenAdapter;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PriceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketFragment extends Fragment {

    private RecyclerView recyclerView;
    private TokenAdapter adapter;
    private List<TokenPrice> tokenList;
    private List<TokenPrice> allTokenList; // Store all tokens for filtering
    private TextInputEditText etSearch;
    private Button btnSortByName;
    private boolean isSortedByName = false;
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
        allTokenList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerViewTokens);
        etSearch = view.findViewById(R.id.etSearch);
        btnSortByName = view.findViewById(R.id.btnSortByName);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TokenAdapter(tokenList, tokenPrice -> {
            // Navigate to TradeFragment when token is clicked
            if (getActivity() instanceof com.koigzzzz.cex.HomeActivity) {
                ((com.koigzzzz.cex.HomeActivity) getActivity()).navigateToTrade(tokenPrice.getSymbol());
            }
        });
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        setupSearch();
        
        // Setup sort button
        btnSortByName.setOnClickListener(v -> toggleSortByName());

        loadEnabledTokens();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTokens(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void toggleSortByName() {
        isSortedByName = !isSortedByName;
        if (isSortedByName) {
            btnSortByName.setText("Sort by Symbol");
        } else {
            btnSortByName.setText("Sort by Name");
        }
        // Re-apply current filter with new sort order
        String searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        filterTokens(searchQuery);
    }

    private void filterTokens(String query) {
        tokenList.clear();
        List<TokenPrice> filteredList = new ArrayList<>();
        
        if (query.isEmpty()) {
            // Show all tokens
            filteredList.addAll(allTokenList);
        } else {
            // Filter by symbol (case-insensitive)
            String queryUpper = query.toUpperCase();
            for (TokenPrice token : allTokenList) {
                // Search by symbol only, not by name
                if (token.getSymbol().toUpperCase().contains(queryUpper)) {
                    filteredList.add(token);
                }
            }
        }
        
        // Apply sorting if enabled
        if (isSortedByName) {
            Collections.sort(filteredList, new Comparator<TokenPrice>() {
                @Override
                public int compare(TokenPrice t1, TokenPrice t2) {
                    // Sort by token name (case-insensitive)
                    String name1 = t1.getName() != null ? t1.getName().toLowerCase() : "";
                    String name2 = t2.getName() != null ? t2.getName().toLowerCase() : "";
                    return name1.compareTo(name2);
                }
            });
        } else {
            // Sort by symbol (default)
            Collections.sort(filteredList, new Comparator<TokenPrice>() {
                @Override
                public int compare(TokenPrice t1, TokenPrice t2) {
                    return t1.getSymbol().compareToIgnoreCase(t2.getSymbol());
                }
            });
        }
        
        tokenList.addAll(filteredList);
        adapter.notifyDataSetChanged();
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
        
        allTokenList.clear();
        // Add tokens in order from enabledTokenSymbols
        // Show all enabled tokens, even if price data is not available
        for (String symbol : enabledTokenSymbols) {
            String symbolUpper = symbol.toUpperCase();
            TokenPrice tokenPrice = prices.get(symbolUpper);
            
            if (tokenPrice != null) {
                // Token has price data
                allTokenList.add(tokenPrice);
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
                allTokenList.add(placeholderPrice);
            }
        }
        
        // Apply current search filter
        String searchQuery = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        filterTokens(searchQuery);
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


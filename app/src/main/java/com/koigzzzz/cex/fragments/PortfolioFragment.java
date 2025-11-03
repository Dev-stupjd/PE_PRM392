package com.koigzzzz.cex.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.koigzzzz.cex.R;
import com.koigzzzz.cex.models.Wallet;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PortfolioValueTracker;
import com.koigzzzz.cex.utils.PriceManager;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

public class PortfolioFragment extends Fragment {

    private TextView tvTotalBalance, tvUsdtBalance;
    private TextView tvBtcBalance, tvBtcValue;
    private TextView tvEthBalance, tvEthValue;
    private TextView tvSolBalance, tvSolValue;
    private TextView tvBnbBalance, tvBnbValue;
    private TextView tvRevenue1d, tvRevenue3d, tvRevenue7d;
    private FirebaseHelper firebaseHelper;
    private PriceManager priceManager;
    private PortfolioValueTracker portfolioValueTracker;
    private String userId;
    private Wallet userWallet;
    
    // Time period constants (in milliseconds)
    private static final long PERIOD_1_DAY = 24 * 60 * 60 * 1000;     // 1 day
    private static final long PERIOD_3_DAYS = 3 * 24 * 60 * 60 * 1000; // 3 days
    private static final long PERIOD_7_DAYS = 7 * 24 * 60 * 60 * 1000; // 7 days

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();
        priceManager = PriceManager.getInstance();
        portfolioValueTracker = PortfolioValueTracker.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        initializeViews(view);
        loadPortfolio();

        return view;
    }

    private void initializeViews(View view) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvUsdtBalance = view.findViewById(R.id.tvUsdtBalance);
        tvBtcBalance = view.findViewById(R.id.tvBtcBalance);
        tvBtcValue = view.findViewById(R.id.tvBtcValue);
        tvEthBalance = view.findViewById(R.id.tvEthBalance);
        tvEthValue = view.findViewById(R.id.tvEthValue);
        tvSolBalance = view.findViewById(R.id.tvSolBalance);
        tvSolValue = view.findViewById(R.id.tvSolValue);
        tvBnbBalance = view.findViewById(R.id.tvBnbBalance);
        tvBnbValue = view.findViewById(R.id.tvBnbValue);
        tvRevenue1d = view.findViewById(R.id.tvRevenue1d);
        tvRevenue3d = view.findViewById(R.id.tvRevenue3d);
        tvRevenue7d = view.findViewById(R.id.tvRevenue7d);
    }

    private void loadPortfolio() {
        if (getContext() == null || userId == null) return;

        firebaseHelper.getUserDocument(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    Map<String, Object> walletMap = (Map<String, Object>) doc.get("wallet");
                    if (walletMap != null) {
                        userWallet = new Wallet();
                        userWallet.loadFromMap(walletMap); // Supports dynamic tokens
                        
                        // Load prices and update display
                        loadPricesAndUpdateDisplay();
                    }
                }
            }
        });
    }

    private void loadPricesAndUpdateDisplay() {
        if (getContext() == null) return;

        priceManager.fetchAllPrices(new PriceManager.AllPricesCallback() {
            @Override
            public void onPricesReceived(Map<String, TokenPrice> prices) {
                if (isAdded() && getContext() != null) {
                    updatePortfolioDisplay(prices);
                }
            }

            @Override
            public void onError(String error) {
                // Use cached prices if available
                if (isAdded() && getContext() != null) {
                    Map<String, TokenPrice> cached = new java.util.HashMap<>();
                    String[] symbols = {"BTC", "ETH", "SOL", "BNB"};
                    for (String symbol : symbols) {
                        TokenPrice cachedPrice = priceManager.getCachedPrice(symbol);
                        if (cachedPrice != null) {
                            cached.put(symbol, cachedPrice);
                        }
                    }
                    if (!cached.isEmpty()) {
                        updatePortfolioDisplay(cached);
                    }
                }
            }
        });
    }

    private void updatePortfolioDisplay(Map<String, TokenPrice> prices) {
        if (userWallet == null || !isAdded() || getContext() == null) return;

        NumberFormat format = new DecimalFormat("#,##0.00");
        NumberFormat tokenFormat = new DecimalFormat("#,##0.00000000");

        // USDT
        tvUsdtBalance.setText("$" + format.format(userWallet.getUsdt()));

        double totalValue = userWallet.getUsdt();

        // BTC
        double btcBalance = userWallet.getBtc();
        tvBtcBalance.setText(tokenFormat.format(btcBalance) + " BTC");
        TokenPrice btcPrice = prices.get("BTC");
        if (btcPrice != null) {
            double btcValue = btcBalance * btcPrice.getPrice();
            tvBtcValue.setText("$" + format.format(btcValue));
            totalValue += btcValue;
        } else {
            tvBtcValue.setText("$0.00");
        }

        // ETH
        double ethBalance = userWallet.getEth();
        tvEthBalance.setText(tokenFormat.format(ethBalance) + " ETH");
        TokenPrice ethPrice = prices.get("ETH");
        if (ethPrice != null) {
            double ethValue = ethBalance * ethPrice.getPrice();
            tvEthValue.setText("$" + format.format(ethValue));
            totalValue += ethValue;
        } else {
            tvEthValue.setText("$0.00");
        }

        // SOL
        double solBalance = userWallet.getSol();
        tvSolBalance.setText(tokenFormat.format(solBalance) + " SOL");
        TokenPrice solPrice = prices.get("SOL");
        if (solPrice != null) {
            double solValue = solBalance * solPrice.getPrice();
            tvSolValue.setText("$" + format.format(solValue));
            totalValue += solValue;
        } else {
            tvSolValue.setText("$0.00");
        }

        // BNB
        double bnbBalance = userWallet.getBnb();
        tvBnbBalance.setText(tokenFormat.format(bnbBalance) + " BNB");
        TokenPrice bnbPrice = prices.get("BNB");
        if (bnbPrice != null) {
            double bnbValue = bnbBalance * bnbPrice.getPrice();
            tvBnbValue.setText("$" + format.format(bnbValue));
            totalValue += bnbValue;
        } else {
            tvBnbValue.setText("$0.00");
        }

        // Total Balance
        tvTotalBalance.setText("$" + format.format(totalValue));
        
        // Track portfolio value for revenue calculation
        if (userId != null) {
            portfolioValueTracker.addPortfolioValue(userId, totalValue);
            
            // TEST: Uncomment the line below to add test historical data for testing
            // This simulates portfolio values from the past so you can see revenue calculations immediately
           // addTestHistoricalData(totalValue);
        }
        
        // Calculate and display revenue
        updateRevenueDisplay(totalValue);
    }
    
    private void updateRevenueDisplay(double currentValue) {
        if (userId == null || !isAdded() || getContext() == null) return;
        
        NumberFormat format = new DecimalFormat("#,##0.00");
        
        // Calculate revenue for each time period
        double revenue1d = portfolioValueTracker.calculateRevenue(userId, currentValue, PERIOD_1_DAY);
        double revenue3d = portfolioValueTracker.calculateRevenue(userId, currentValue, PERIOD_3_DAYS);
        double revenue7d = portfolioValueTracker.calculateRevenue(userId, currentValue, PERIOD_7_DAYS);
        
        // Display 1 day revenue
        String revenue1dText;
        if (revenue1d == 0.0) {
            revenue1dText = "$0.00";
        } else {
            String sign = revenue1d >= 0 ? "+" : "-";
            revenue1dText = sign + "$" + format.format(Math.abs(revenue1d));
        }
        tvRevenue1d.setText(revenue1dText);
        tvRevenue1d.setTextColor(revenue1d >= 0 ? 
                getResources().getColor(R.color.binance_green, null) : 
                getResources().getColor(R.color.binance_red, null));
        
        // Display 3 days revenue
        String revenue3dText;
        if (revenue3d == 0.0) {
            revenue3dText = "$0.00";
        } else {
            String sign = revenue3d >= 0 ? "+" : "-";
            revenue3dText = sign + "$" + format.format(Math.abs(revenue3d));
        }
        tvRevenue3d.setText(revenue3dText);
        tvRevenue3d.setTextColor(revenue3d >= 0 ? 
                getResources().getColor(R.color.binance_green, null) : 
                getResources().getColor(R.color.binance_red, null));
        
        // Display 7 days revenue
        String revenue7dText;
        if (revenue7d == 0.0) {
            revenue7dText = "$0.00";
        } else {
            String sign = revenue7d >= 0 ? "+" : "-";
            revenue7dText = sign + "$" + format.format(Math.abs(revenue7d));
        }
        tvRevenue7d.setText(revenue7dText);
        tvRevenue7d.setTextColor(revenue7d >= 0 ? 
                getResources().getColor(R.color.binance_green, null) : 
                getResources().getColor(R.color.binance_red, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPortfolio();
    }
    
    /**
     * TEST METHOD: Add historical portfolio values for testing revenue calculation
     * This simulates historical data so you can see revenue calculations immediately
     * 
     * Test scenario:
     * - 1 day ago: portfolio was $100 higher than now → 24h revenue should be negative
     * - 3 days ago: portfolio was $200 lower than now → 3d revenue should be positive  
     * - 7 days ago: portfolio was $500 lower than now → 7d revenue should be positive
     */
    private void addTestHistoricalData(double currentValue) {
        if (userId == null) return;
        
        long now = System.currentTimeMillis();
        
        // Add test data points with timestamps in the past
        portfolioValueTracker.addPortfolioValue(userId, currentValue + 100, now - (1 * 24 * 60 * 60 * 1000L)); // 1 day ago: $100 more
        portfolioValueTracker.addPortfolioValue(userId, currentValue - 200, now - (3 * 24 * 60 * 60 * 1000L)); // 3 days ago: $200 less
        portfolioValueTracker.addPortfolioValue(userId, currentValue - 500, now - (7 * 24 * 60 * 60 * 1000L)); // 7 days ago: $500 less
        
        // Note: updateRevenueDisplay will be called after this method returns
    }
}


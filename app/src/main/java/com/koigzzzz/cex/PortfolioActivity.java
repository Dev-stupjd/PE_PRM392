package com.koigzzzz.cex;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.koigzzzz.cex.models.Wallet;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.utils.FirebaseHelper;
import com.koigzzzz.cex.utils.PriceManager;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

public class PortfolioActivity extends AppCompatActivity {

    private TextView tvTotalBalance, tvUsdtBalance;
    private TextView tvBtcBalance, tvBtcValue;
    private TextView tvEthBalance, tvEthValue;
    private TextView tvSolBalance, tvSolValue;
    private TextView tvBnbBalance, tvBnbValue;
    private FirebaseHelper firebaseHelper;
    private PriceManager priceManager;
    private BottomNavigationView bottomNavigation;
    private String userId;
    private Wallet userWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_portfolio);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseHelper = FirebaseHelper.getInstance();
        priceManager = PriceManager.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupNavigation();
        loadPortfolio();
    }

    private void initializeViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvUsdtBalance = findViewById(R.id.tvUsdtBalance);
        tvBtcBalance = findViewById(R.id.tvBtcBalance);
        tvBtcValue = findViewById(R.id.tvBtcValue);
        tvEthBalance = findViewById(R.id.tvEthBalance);
        tvEthValue = findViewById(R.id.tvEthValue);
        tvSolBalance = findViewById(R.id.tvSolBalance);
        tvSolValue = findViewById(R.id.tvSolValue);
        tvBnbBalance = findViewById(R.id.tvBnbBalance);
        tvBnbValue = findViewById(R.id.tvBnbValue);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_market) {
                    Intent intent = new Intent(PortfolioActivity.this, MarketActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_portfolio) {
                    // Already here
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    Intent intent = new Intent(PortfolioActivity.this, OrdersActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(PortfolioActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadPortfolio() {
        firebaseHelper.getUserDocument(userId, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    Map<String, Object> walletMap = (Map<String, Object>) doc.get("wallet");
                    if (walletMap != null) {
                        userWallet = new Wallet();
                        userWallet.setUsdt(((Number) walletMap.get("usdt")).doubleValue());
                        userWallet.setBtc(((Number) walletMap.get("btc")).doubleValue());
                        userWallet.setEth(((Number) walletMap.get("eth")).doubleValue());
                        userWallet.setSol(((Number) walletMap.get("sol")).doubleValue());
                        userWallet.setBnb(((Number) walletMap.get("bnb")).doubleValue());
                        
                        // Load prices and update display
                        loadPricesAndUpdateDisplay();
                    }
                }
            }
        });
    }

    private void loadPricesAndUpdateDisplay() {
        priceManager.fetchAllPrices(new PriceManager.AllPricesCallback() {
            @Override
            public void onPricesReceived(Map<String, TokenPrice> prices) {
                updatePortfolioDisplay(prices);
            }

            @Override
            public void onError(String error) {
                // Use cached prices if available
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
        });
    }

    private void updatePortfolioDisplay(Map<String, TokenPrice> prices) {
        if (userWallet == null) return;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPortfolio();
    }
}


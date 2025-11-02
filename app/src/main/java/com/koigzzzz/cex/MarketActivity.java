package com.koigzzzz.cex;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.koigzzzz.cex.adapters.TokenAdapter;
import com.koigzzzz.cex.models.TokenPrice;
import com.koigzzzz.cex.utils.PriceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TokenAdapter adapter;
    private List<TokenPrice> tokenList;
    private PriceManager priceManager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_market);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        priceManager = PriceManager.getInstance();
        tokenList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerViewTokens);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TokenAdapter(tokenList, tokenPrice -> {
            // Navigate to TradeActivity when token is clicked
            Intent intent = new Intent(MarketActivity.this, TradeActivity.class);
            intent.putExtra("SYMBOL", tokenPrice.getSymbol());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_market) {
                    // Already here
                    return true;
                } else if (itemId == R.id.nav_portfolio) {
                    Intent intent = new Intent(MarketActivity.this, PortfolioActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    Intent intent = new Intent(MarketActivity.this, OrdersActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(MarketActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        loadTokenPrices();
    }

    private void loadTokenPrices() {
        // Use cached prices first for immediate display
        Map<String, TokenPrice> cached = new HashMap<>();
        String[] symbols = {"BTC", "ETH", "SOL", "BNB"};
        for (String symbol : symbols) {
            TokenPrice cachedPrice = priceManager.getCachedPrice(symbol);
            if (cachedPrice != null) {
                cached.put(symbol, cachedPrice);
            }
        }
        
        if (!cached.isEmpty()) {
            updateTokenList(cached);
        }
        
        priceManager.fetchAllPrices(new PriceManager.AllPricesCallback() {
            @Override
            public void onPricesReceived(Map<String, TokenPrice> prices) {
                updateTokenList(prices);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MarketActivity.this, "Error loading prices: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateTokenList(Map<String, TokenPrice> prices) {
        tokenList.clear();
        // Add tokens in order: BTC, ETH, SOL, BNB
        String[] symbols = {"BTC", "ETH", "SOL", "BNB"};
        for (String symbol : symbols) {
            TokenPrice tokenPrice = prices.get(symbol);
            if (tokenPrice != null) {
                tokenList.add(tokenPrice);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh prices when returning to this activity
        loadTokenPrices();
    }
}

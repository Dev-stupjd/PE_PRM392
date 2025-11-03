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
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.koigzzzz.cex.fragments.AdminFragment;
import com.koigzzzz.cex.fragments.MarketFragment;
import com.koigzzzz.cex.fragments.OrdersFragment;
import com.koigzzzz.cex.fragments.PortfolioFragment;
import com.koigzzzz.cex.fragments.ProfileFragment;
import com.koigzzzz.cex.fragments.TradeFragment;
import com.koigzzzz.cex.utils.FirebaseHelper;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if user is logged in
        FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
        if (firebaseHelper.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // Show/hide menu items based on admin status
        boolean isAdmin = firebaseHelper.isAdmin();
        if (isAdmin) {
            // Admin: Show only Admin and Logout tabs
            bottomNavigation.getMenu().findItem(R.id.nav_market).setVisible(false);
            bottomNavigation.getMenu().findItem(R.id.nav_portfolio).setVisible(false);
            bottomNavigation.getMenu().findItem(R.id.nav_orders).setVisible(false);
            bottomNavigation.getMenu().findItem(R.id.nav_profile).setVisible(false);
            bottomNavigation.getMenu().findItem(R.id.nav_admin).setVisible(true);
        } else {
            // Regular user: Show Market, Portfolio, Orders, Profile, hide Admin
            bottomNavigation.getMenu().findItem(R.id.nav_market).setVisible(true);
            bottomNavigation.getMenu().findItem(R.id.nav_portfolio).setVisible(true);
            bottomNavigation.getMenu().findItem(R.id.nav_orders).setVisible(true);
            bottomNavigation.getMenu().findItem(R.id.nav_profile).setVisible(true);
            bottomNavigation.getMenu().findItem(R.id.nav_admin).setVisible(false);
        }
        
        // Initialize API key
        com.koigzzzz.cex.utils.PriceManager priceManager = com.koigzzzz.cex.utils.PriceManager.getInstance();
        String apiKey = getString(R.string.livecoinwatch_api_key);
        if (apiKey != null && !apiKey.equals("YOUR_API_KEY_HERE")) {
            priceManager.setApiKey(apiKey);
        }

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                Fragment fragment = null;

                if (itemId == R.id.nav_market) {
                    fragment = new MarketFragment();
                } else if (itemId == R.id.nav_portfolio) {
                    fragment = new PortfolioFragment();
                } else if (itemId == R.id.nav_orders) {
                    fragment = new OrdersFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                } else if (itemId == R.id.nav_admin) {
                    if (firebaseHelper.isAdmin()) {
                        fragment = new AdminFragment();
                    } else {
                        Toast.makeText(HomeActivity.this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else if (itemId == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }

                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });

        // Set default fragment based on user type
        if (savedInstanceState == null) {
            Fragment defaultFragment;
            if (isAdmin) {
                defaultFragment = new AdminFragment();
                // Select admin menu item
                bottomNavigation.setSelectedItemId(R.id.nav_admin);
            } else {
                defaultFragment = new MarketFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, defaultFragment)
                    .commit();
        }

        // Handle intent for TradeFragment (e.g., from MarketFragment clicking a token)
        String symbol = getIntent().getStringExtra("SYMBOL");
        if (symbol != null) {
            TradeFragment tradeFragment = TradeFragment.newInstance(symbol);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, tradeFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void navigateToTrade(String symbol) {
        TradeFragment tradeFragment = TradeFragment.newInstance(symbol);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, tradeFragment)
                .addToBackStack(null)
                .commit();
    }
}


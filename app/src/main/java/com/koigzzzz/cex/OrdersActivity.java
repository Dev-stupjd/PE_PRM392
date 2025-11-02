package com.koigzzzz.cex;

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
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.adapters.OrderAdapter;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private FirebaseHelper firebaseHelper;
    private BottomNavigationView bottomNavigation;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseHelper = FirebaseHelper.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerViewOrders);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(orderList);
        recyclerView.setAdapter(adapter);

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_market) {
                    Intent intent = new Intent(OrdersActivity.this, MarketActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_portfolio) {
                    Intent intent = new Intent(OrdersActivity.this, PortfolioActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.nav_orders) {
                    // Already here
                    return true;
                } else if (itemId == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(OrdersActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            }
        });

        loadOrders();
    }

    private void loadOrders() {
        firebaseHelper.getUserOrders(userId, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                QuerySnapshot querySnapshot = task.getResult();
                orderList.clear();
                
                // Convert documents to Order objects
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        Order order = new Order();
                        order.setOrderId(doc.getString("orderId"));
                        order.setUserId(doc.getString("userId"));
                        order.setType(doc.getString("type"));
                        order.setOrderType(doc.getString("orderType") != null ? doc.getString("orderType") : Order.ORDER_TYPE_LIMIT);
                        order.setSymbol(doc.getString("symbol"));
                        
                        Object qtyObj = doc.get("quantity");
                        Object priceObj = doc.get("price");
                        Object totalObj = doc.get("total");
                        Object timestampObj = doc.get("timestamp");
                        
                        if (qtyObj != null) order.setQuantity(((Number) qtyObj).doubleValue());
                        if (priceObj != null) order.setPrice(((Number) priceObj).doubleValue());
                        if (totalObj != null) order.setTotal(((Number) totalObj).doubleValue());
                        if (timestampObj != null) order.setTimestamp(((Number) timestampObj).longValue());
                        
                        order.setStatus(doc.getString("status"));
                        orderList.add(order);
                    } catch (Exception e) {
                        Log.e("OrdersActivity", "Error parsing order: " + doc.getId(), e);
                    }
                }
                
                // Sort by timestamp descending (most recent first)
                orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                
                adapter.notifyDataSetChanged();
                
                if (orderList.isEmpty()) {
                    Toast.makeText(OrdersActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                }
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(OrdersActivity.this, "Error loading orders: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }
}

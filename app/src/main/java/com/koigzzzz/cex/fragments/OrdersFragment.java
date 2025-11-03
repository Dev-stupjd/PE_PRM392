package com.koigzzzz.cex.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.koigzzzz.cex.adapters.OrderAdapter;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private FirebaseHelper firebaseHelper;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = FirebaseHelper.getInstance();
        userId = firebaseHelper.getCurrentUser() != null ? firebaseHelper.getCurrentUser().getUid() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return view;
        }

        orderList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerViewOrders);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderAdapter(orderList);
        adapter.setOnCancelOrderListener(order -> cancelOrder(order));
        recyclerView.setAdapter(adapter);

        loadOrders();

        return view;
    }

    private void loadOrders() {
        if (getContext() == null || userId == null) return;

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
                        Log.e("OrdersFragment", "Error parsing order: " + doc.getId(), e);
                    }
                }
                
                // Sort by timestamp descending (most recent first)
                orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                
                if (isAdded() && getContext() != null) {
                    adapter.notifyDataSetChanged();
                    
                    if (orderList.isEmpty()) {
                        Toast.makeText(getContext(), "No orders found", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (isAdded() && getContext() != null) {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    Toast.makeText(getContext(), "Error loading orders: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void cancelOrder(Order order) {
        if (getContext() == null || userId == null) return;

        // Only allow canceling pending orders
        if (!order.getStatus().equals(Order.STATUS_PENDING)) {
            Toast.makeText(getContext(), "Only pending orders can be cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current wallet to refund the reserved funds
        firebaseHelper.getUserDocument(userId, getUserTask -> {
            if (!getUserTask.isSuccessful() || getUserTask.getResult() == null) {
                Toast.makeText(getContext(), "Error loading wallet", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.firestore.DocumentSnapshot walletDoc = getUserTask.getResult();
            if (!walletDoc.exists()) {
                Toast.makeText(getContext(), "Wallet not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> walletMap = (Map<String, Object>) walletDoc.get("wallet");
            if (walletMap == null) {
                Toast.makeText(getContext(), "Wallet data not found", Toast.LENGTH_SHORT).show();
                return;
            }

            com.koigzzzz.cex.models.Wallet wallet = new com.koigzzzz.cex.models.Wallet();
            wallet.setUsdt(((Number) walletMap.get("usdt")).doubleValue());
            wallet.setBtc(((Number) walletMap.get("btc")).doubleValue());
            wallet.setEth(((Number) walletMap.get("eth")).doubleValue());
            wallet.setSol(((Number) walletMap.get("sol")).doubleValue());
            wallet.setBnb(((Number) walletMap.get("bnb")).doubleValue());

            double total = order.getPrice() * order.getQuantity();

            // Refund reserved funds
            if (order.getType().equals(Order.TYPE_BUY)) {
                // Refund USDT that was reserved for buy order
                wallet.setUsdt(wallet.getUsdt() + total);
            } else {
                // Refund tokens that were reserved for sell order
                wallet.setBalance(order.getSymbol(), wallet.getBalance(order.getSymbol()) + order.getQuantity());
            }

            // Update order status to cancelled
            firebaseHelper.updateOrderStatus(order.getOrderId(), Order.STATUS_CANCELLED, updateTask -> {
                if (updateTask.isSuccessful()) {
                    // Update wallet with refunded funds
                    firebaseHelper.updateWallet(userId, wallet.toMap(), updateWalletTask -> {
                        if (isAdded() && getContext() != null) {
                            if (updateWalletTask.isSuccessful()) {
                                Toast.makeText(getContext(), "Order cancelled", Toast.LENGTH_SHORT).show();
                                loadOrders(); // Reload orders list
                            } else {
                                Toast.makeText(getContext(), "Error refunding funds", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error cancelling order", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}


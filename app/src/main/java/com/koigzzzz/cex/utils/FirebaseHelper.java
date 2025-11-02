package com.koigzzzz.cex.utils;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.koigzzzz.cex.models.Order;
import com.koigzzzz.cex.models.User;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void createUserDocument(User user, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(user.getUserId())
                .set(user.toMap())
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error creating user document", e));
    }

    public void getUserDocument(String userId, OnCompleteListener<DocumentSnapshot> listener) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user document", e));
    }

    public void updateWallet(String userId, Map<String, Object> walletData, OnCompleteListener<Void> listener) {
        db.collection("users")
                .document(userId)
                .update("wallet", walletData)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating wallet", e));
    }

    public void createOrder(Order order, OnCompleteListener<Void> listener) {
        db.collection("orders")
                .document(order.getOrderId())
                .set(order.toMap())
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error creating order", e));
    }

    public void getUserOrders(String userId, OnCompleteListener<QuerySnapshot> listener) {
        // Remove orderBy to avoid composite index requirement - will sort in memory instead
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user orders", e));
    }

    public void updateOrderStatus(String orderId, String status, OnCompleteListener<Void> listener) {
        db.collection("orders")
                .document(orderId)
                .update("status", status)
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating order status", e));
    }

    public void getAllOrders(String symbol, OnCompleteListener<QuerySnapshot> listener) {
        db.collection("orders")
                .whereEqualTo("symbol", symbol)
                .whereEqualTo("status", Order.STATUS_PENDING)
                .orderBy("price", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener(e -> Log.e(TAG, "Error getting orders", e));
    }
}


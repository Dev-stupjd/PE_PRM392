package com.koigzzzz.cex.models;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String username;
    private Wallet wallet;
    private long createdAt;

    public User() {
        // Default constructor required for Firestore
    }

    public User(String userId, String email, String username) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.wallet = new Wallet();
        this.createdAt = System.currentTimeMillis();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("username", username);
        map.put("wallet", wallet != null ? wallet.toMap() : new Wallet().toMap());
        map.put("createdAt", createdAt);
        return map;
    }
}


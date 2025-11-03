package com.koigzzzz.cex.models;

import java.util.HashMap;
import java.util.Map;

public class Token {
    private String tokenId;
    private String symbol;
    private String name;
    private boolean enabled;
    private long createdAt;
    private long updatedAt;

    public Token() {
        // Default constructor for Firestore
    }

    public Token(String symbol, String name, boolean enabled) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.enabled = enabled;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol != null ? symbol.toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("symbol", symbol);
        map.put("name", name);
        map.put("enabled", enabled);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}


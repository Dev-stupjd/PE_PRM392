package com.koigzzzz.cex.models;

import java.util.HashMap;
import java.util.Map;

public class Wallet {
    // Internal map to store all token balances (supports dynamic tokens)
    private Map<String, Double> balances;

    public Wallet() {
        balances = new HashMap<>();
        // Starting balance
        balances.put("usdt", 10000.0);
        balances.put("btc", 0.0);
        balances.put("eth", 0.0);
        balances.put("sol", 0.0);
        balances.put("bnb", 0.0);
    }

    // Getters for backward compatibility
    public double getUsdt() {
        return balances.getOrDefault("usdt", 0.0);
    }

    public void setUsdt(double usdt) {
        balances.put("usdt", usdt);
    }

    public double getBtc() {
        return balances.getOrDefault("btc", 0.0);
    }

    public void setBtc(double btc) {
        balances.put("btc", btc);
    }

    public double getEth() {
        return balances.getOrDefault("eth", 0.0);
    }

    public void setEth(double eth) {
        balances.put("eth", eth);
    }

    public double getSol() {
        return balances.getOrDefault("sol", 0.0);
    }

    public void setSol(double sol) {
        balances.put("sol", sol);
    }

    public double getBnb() {
        return balances.getOrDefault("bnb", 0.0);
    }

    public void setBnb(double bnb) {
        balances.put("bnb", bnb);
    }

    // Dynamic token balance methods
    public double getBalance(String symbol) {
        String key = symbol.toLowerCase();
        return balances.getOrDefault(key, 0.0);
    }

    public void setBalance(String symbol, double amount) {
        String key = symbol.toLowerCase();
        balances.put(key, amount);
    }

    // Load wallet from Firestore map (supports dynamic tokens)
    public void loadFromMap(Map<String, Object> walletMap) {
        if (walletMap == null) return;
        
        balances.clear();
        // Starting balance
        balances.put("usdt", 10000.0);
        balances.put("btc", 0.0);
        balances.put("eth", 0.0);
        balances.put("sol", 0.0);
        balances.put("bnb", 0.0);
        
        // Load all balances from Firestore (supports any token)
        for (Map.Entry<String, Object> entry : walletMap.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            if (value instanceof Number) {
                balances.put(key, ((Number) value).doubleValue());
            }
        }
    }

    public Map<String, Object> toMap() {
        // Return a copy of the balances map
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}


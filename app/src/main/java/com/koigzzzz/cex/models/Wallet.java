package com.koigzzzz.cex.models;

import java.util.HashMap;
import java.util.Map;

public class Wallet {
    private double usdt;
    private double btc;
    private double eth;
    private double sol;
    private double bnb;

    public Wallet() {
        this.usdt = 10000.0; // Starting balance
        this.btc = 0.0;
        this.eth = 0.0;
        this.sol = 0.0;
        this.bnb = 0.0;
    }

    public double getUsdt() {
        return usdt;
    }

    public void setUsdt(double usdt) {
        this.usdt = usdt;
    }

    public double getBtc() {
        return btc;
    }

    public void setBtc(double btc) {
        this.btc = btc;
    }

    public double getEth() {
        return eth;
    }

    public void setEth(double eth) {
        this.eth = eth;
    }

    public double getSol() {
        return sol;
    }

    public void setSol(double sol) {
        this.sol = sol;
    }

    public double getBnb() {
        return bnb;
    }

    public void setBnb(double bnb) {
        this.bnb = bnb;
    }

    public double getBalance(String symbol) {
        switch (symbol.toUpperCase()) {
            case "USDT": return usdt;
            case "BTC": return btc;
            case "ETH": return eth;
            case "SOL": return sol;
            case "BNB": return bnb;
            default: return 0.0;
        }
    }

    public void setBalance(String symbol, double amount) {
        switch (symbol.toUpperCase()) {
            case "USDT": this.usdt = amount; break;
            case "BTC": this.btc = amount; break;
            case "ETH": this.eth = amount; break;
            case "SOL": this.sol = amount; break;
            case "BNB": this.bnb = amount; break;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("usdt", usdt);
        map.put("btc", btc);
        map.put("eth", eth);
        map.put("sol", sol);
        map.put("bnb", bnb);
        return map;
    }
}


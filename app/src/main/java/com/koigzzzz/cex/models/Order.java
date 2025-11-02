package com.koigzzzz.cex.models;

import java.util.HashMap;
import java.util.Map;

public class Order {
    public static final String TYPE_BUY = "BUY";
    public static final String TYPE_SELL = "SELL";
    public static final String ORDER_TYPE_LIMIT = "LIMIT";
    public static final String ORDER_TYPE_MARKET = "MARKET";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String orderId;
    private String userId;
    private String type; // BUY or SELL
    private String orderType; // LIMIT or MARKET
    private String symbol; // BTC, ETH, SOL, BNB
    private double quantity;
    private double price;
    private double total;
    private String status;
    private long timestamp;

    public Order() {
        // Default constructor required for Firestore
    }

    public Order(String orderId, String userId, String type, String symbol, double quantity, double price) {
        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.orderType = ORDER_TYPE_LIMIT; // Default to limit
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.total = quantity * price;
        this.status = STATUS_PENDING;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Order(String orderId, String userId, String type, String orderType, String symbol, double quantity, double price) {
        this.orderId = orderId;
        this.userId = userId;
        this.type = type;
        this.orderType = orderType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.total = quantity * price;
        this.status = STATUS_PENDING;
        this.timestamp = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("userId", userId);
        map.put("type", type);
        map.put("orderType", orderType);
        map.put("symbol", symbol);
        map.put("quantity", quantity);
        map.put("price", price);
        map.put("total", total);
        map.put("status", status);
        map.put("timestamp", timestamp);
        return map;
    }
}


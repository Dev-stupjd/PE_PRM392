package com.koigzzzz.cex.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks price history for tokens over time.
 * Stores price points with timestamps to enable charting.
 */
public class PriceHistoryTracker {
    private static PriceHistoryTracker instance;
    
    // Maximum number of data points to keep per token (last 50 points = ~4 hours if refreshed every 5 min)
    private static final int MAX_DATA_POINTS = 50;
    
    // Data structure: symbol -> List of PricePoint
    private Map<String, List<PricePoint>> priceHistory;
    
    private PriceHistoryTracker() {
        priceHistory = new HashMap<>();
    }
    
    public static synchronized PriceHistoryTracker getInstance() {
        if (instance == null) {
            instance = new PriceHistoryTracker();
        }
        return instance;
    }
    
    /**
     * Add a new price point for a token
     */
    public void addPricePoint(String symbol, double price) {
        String symbolUpper = symbol.toUpperCase();
        
        List<PricePoint> history = priceHistory.get(symbolUpper);
        if (history == null) {
            history = new ArrayList<>();
            priceHistory.put(symbolUpper, history);
        }
        
        // Add new price point
        history.add(new PricePoint(price, System.currentTimeMillis()));
        
        // Keep only the most recent MAX_DATA_POINTS
        if (history.size() > MAX_DATA_POINTS) {
            history.remove(0);
        }
    }
    
    /**
     * Get price history for a token
     */
    public List<PricePoint> getPriceHistory(String symbol) {
        String symbolUpper = symbol.toUpperCase();
        List<PricePoint> history = priceHistory.get(symbolUpper);
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history); // Return a copy
    }
    
    /**
     * Get price values only (for charting)
     */
    public List<Float> getPriceValues(String symbol) {
        List<PricePoint> history = getPriceHistory(symbol);
        List<Float> values = new ArrayList<>();
        for (PricePoint point : history) {
            values.add((float) point.price);
        }
        return values;
    }
    
    /**
     * Get price history filtered by time frame (milliseconds)
     * @param symbol Token symbol
     * @param timeFrameMs Time frame in milliseconds (e.g., 5 minutes = 5 * 60 * 1000)
     * @return List of price points within the time frame, sorted by timestamp
     */
    public List<PricePoint> getPriceHistoryByTimeFrame(String symbol, long timeFrameMs) {
        List<PricePoint> allHistory = getPriceHistory(symbol);
        if (allHistory.isEmpty()) {
            return new ArrayList<>();
        }
        
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - timeFrameMs;
        
        List<PricePoint> filtered = new ArrayList<>();
        for (PricePoint point : allHistory) {
            if (point.timestamp >= cutoffTime) {
                filtered.add(point);
            }
        }
        
        // Sort by timestamp to ensure chronological order
        filtered.sort((p1, p2) -> Long.compare(p1.timestamp, p2.timestamp));
        
        return filtered;
    }
    
    /**
     * Get price values filtered by time frame (for charting)
     * @param symbol Token symbol
     * @param timeFrameMs Time frame in milliseconds
     * @return List of price values within the time frame
     */
    public List<Float> getPriceValuesByTimeFrame(String symbol, long timeFrameMs) {
        List<PricePoint> filtered = getPriceHistoryByTimeFrame(symbol, timeFrameMs);
        List<Float> values = new ArrayList<>();
        for (PricePoint point : filtered) {
            values.add((float) point.price);
        }
        return values;
    }
    
    /**
     * Clear history for a token
     */
    public void clearHistory(String symbol) {
        String symbolUpper = symbol.toUpperCase();
        priceHistory.remove(symbolUpper);
    }
    
    /**
     * Clear all history
     */
    public void clearAllHistory() {
        priceHistory.clear();
    }
    
    /**
     * Data class to store a single price point with timestamp
     */
    public static class PricePoint {
        public double price;
        public long timestamp;
        
        public PricePoint(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}


package com.koigzzzz.cex.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks portfolio value over time for revenue calculation.
 */
public class PortfolioValueTracker {
    private static PortfolioValueTracker instance;
    
    // Maximum number of data points to keep (for 7 days, checking hourly = 168 points)
    private static final int MAX_DATA_POINTS = 200;
    
    // Data structure: userId -> List of PortfolioValuePoint
    private Map<String, List<PortfolioValuePoint>> portfolioHistory;
    
    private PortfolioValueTracker() {
        portfolioHistory = new HashMap<>();
    }
    
    public static synchronized PortfolioValueTracker getInstance() {
        if (instance == null) {
            instance = new PortfolioValueTracker();
        }
        return instance;
    }
    
    /**
     * Add a new portfolio value point for a user
     */
    public void addPortfolioValue(String userId, double portfolioValue) {
        addPortfolioValue(userId, portfolioValue, System.currentTimeMillis());
    }
    
    /**
     * Add a portfolio value point with a specific timestamp (useful for testing)
     */
    public void addPortfolioValue(String userId, double portfolioValue, long timestamp) {
        List<PortfolioValuePoint> history = portfolioHistory.get(userId);
        if (history == null) {
            history = new ArrayList<>();
            portfolioHistory.put(userId, history);
        }
        
        // Add new value point
        history.add(new PortfolioValuePoint(portfolioValue, timestamp));
        
        // Keep only the most recent MAX_DATA_POINTS
        if (history.size() > MAX_DATA_POINTS) {
            history.remove(0);
        }
        
        // Sort by timestamp to maintain chronological order
        history.sort((p1, p2) -> Long.compare(p1.timestamp, p2.timestamp));
    }
    
    /**
     * Get portfolio value history for a user
     */
    public List<PortfolioValuePoint> getPortfolioHistory(String userId) {
        List<PortfolioValuePoint> history = portfolioHistory.get(userId);
        if (history == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(history); // Return a copy
    }
    
    /**
     * Get portfolio value at a specific time (or closest available)
     * @param userId User ID
     * @param timeAgoMs Time ago in milliseconds
     * @return Portfolio value at that time, or -1 if not available
     */
    public double getPortfolioValueAt(String userId, long timeAgoMs) {
        List<PortfolioValuePoint> history = getPortfolioHistory(userId);
        if (history.isEmpty()) {
            return -1;
        }
        
        long targetTime = System.currentTimeMillis() - timeAgoMs;
        
        // Find the closest value point before or at the target time
        PortfolioValuePoint closest = null;
        long minDiff = Long.MAX_VALUE;
        
        for (PortfolioValuePoint point : history) {
            if (point.timestamp <= targetTime) {
                long diff = targetTime - point.timestamp;
                if (diff < minDiff) {
                    minDiff = diff;
                    closest = point;
                }
            }
        }
        
        // If no point before target time, use the oldest point
        if (closest == null) {
            closest = history.get(0);
        }
        
        return closest.value;
    }
    
    /**
     * Calculate revenue for a specific time period
     * @param userId User ID
     * @param currentValue Current portfolio value
     * @param timePeriodMs Time period in milliseconds (1 day, 3 days, 7 days)
     * @return Revenue amount (current value - value at time period ago)
     */
    public double calculateRevenue(String userId, double currentValue, long timePeriodMs) {
        double pastValue = getPortfolioValueAt(userId, timePeriodMs);
        if (pastValue < 0) {
            // No historical data available
            return 0.0;
        }
        return currentValue - pastValue;
    }
    
    /**
     * Clear history for a user
     */
    public void clearHistory(String userId) {
        portfolioHistory.remove(userId);
    }
    
    /**
     * Clear all history
     */
    public void clearAllHistory() {
        portfolioHistory.clear();
    }
    
    /**
     * Data class to store a single portfolio value point with timestamp
     */
    public static class PortfolioValuePoint {
        public double value;
        public long timestamp;
        
        public PortfolioValuePoint(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}


package com.koigzzzz.cex.models;

import com.google.gson.annotations.SerializedName;

public class LiveCoinWatchResponse {
    @SerializedName("name")
    private String name;
    
    @SerializedName("symbol")
    private String symbol;
    
    @SerializedName("rate")
    private double rate;
    
    @SerializedName("volume")
    private double volume;
    
    @SerializedName("delta")
    private Delta delta;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public Delta getDelta() {
        return delta;
    }

    public void setDelta(Delta delta) {
        this.delta = delta;
    }

    public static class Delta {
        @SerializedName("day")
        private double day;

        public double getDay() {
            return day;
        }

        public void setDay(double day) {
            this.day = day;
        }
    }
}


package com.koigzzzz.cex.models;

public class TokenPrice {
    private String symbol;
    private String name;
    private double price;
    private double change24h;
    private double volume24h;

    public TokenPrice() {
    }

    public TokenPrice(String symbol, String name, double price, double change24h, double volume24h) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change24h = change24h;
        this.volume24h = volume24h;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChange24h() {
        return change24h;
    }

    public void setChange24h(double change24h) {
        this.change24h = change24h;
    }

    public double getVolume24h() {
        return volume24h;
    }

    public void setVolume24h(double volume24h) {
        this.volume24h = volume24h;
    }
}


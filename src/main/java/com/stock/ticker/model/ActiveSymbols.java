package com.stock.ticker.model;

public record ActiveSymbols(
        String symbol,
        boolean active
) {
    public static ActiveSymbols getActiveSymbol(String symbol, boolean active){
        return new ActiveSymbols(symbol, active);
    }
}

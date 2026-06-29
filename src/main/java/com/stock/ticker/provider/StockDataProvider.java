package com.stock.ticker.provider;

import com.stock.ticker.model.StockSnapshot;

/**
 * Abstraction over market data sources (Yahoo Finance today; swap or add providers later).
 */
public interface StockDataProvider {

    String getProviderName();

    StockSnapshot fetch(String symbol) throws Exception;
}

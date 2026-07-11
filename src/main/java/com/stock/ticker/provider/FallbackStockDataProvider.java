package com.stock.ticker.provider;

import com.crazzyghost.alphavantage.AlphaVantageException;
import com.stock.ticker.model.StockSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


public class FallbackStockDataProvider implements StockDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FallbackStockDataProvider.class);

    private final AlphaVantageProvider alphaVantageProvider;
    private final YahooFinanceProvider yahooFinanceProvider;

    public FallbackStockDataProvider(
            AlphaVantageProvider alphaVantageProvider,
            YahooFinanceProvider yahooFinanceProvider
    ) {
        this.alphaVantageProvider = alphaVantageProvider;
        this.yahooFinanceProvider = yahooFinanceProvider;
    }

    @Override
    public String getProviderName() {
        return alphaVantageProvider.getProviderName() + " (fallback: " + yahooFinanceProvider.getProviderName() + ")";
    }

    @Override
    public StockSnapshot fetch(String symbol) throws Exception {
        try {
            return alphaVantageProvider.fetch(symbol);
        } catch (Exception ex) {
            if (!shouldFallback(ex)) {
                throw ex;
            }
            log.warn("Alpha Vantage failed for {} ({}), falling back to Yahoo Finance",
                    symbol, ex.getMessage());
            return yahooFinanceProvider.fetch(symbol);
        }
    }

    private static boolean shouldFallback(Exception ex) {
        if (ex instanceof IllegalStateException || ex instanceof AlphaVantageException) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("rate limit")
                || lower.contains("api call frequency")
                || lower.contains("thank you for using alpha vantage")
                || lower.contains("premium");
    }
}

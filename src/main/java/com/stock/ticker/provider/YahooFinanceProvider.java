package com.stock.ticker.provider;

import com.stock.ticker.model.StockSnapshot;
import org.springframework.stereotype.Component;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class YahooFinanceProvider implements StockDataProvider {

    @Override
    public String getProviderName() {
        return "yahoo-finance";
    }

    @Override
    public StockSnapshot fetch(String symbol) throws Exception {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null) {
            throw new IllegalArgumentException("No data returned for symbol: " + symbol);
        }

        return new StockSnapshot(
                stock.getSymbol(),
                stock.getName(),
                stock.getCurrency(),
                stock.getStockExchange(),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getPrice() : null),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getPreviousClose() : null),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getDayLow() : null),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getDayHigh() : null),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getYearLow() : null),
                toBigDecimal(stock.getQuote() != null ? stock.getQuote().getYearHigh() : null),
                stock.getQuote() != null ? stock.getQuote().getVolume() : null,
                stock.getStats() != null ? stock.getStats().getMarketCap() : null,
                stock.getStats() != null ? stock.getStats().getPe() : null,
                stock.getDividend() != null ? stock.getDividend().getAnnualYieldPercent() : null,
                null,
                Instant.now(),
                null
        );
    }

    private static BigDecimal toBigDecimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }
}

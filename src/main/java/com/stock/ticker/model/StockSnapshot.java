package com.stock.ticker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockSnapshot(
        String symbol,
        String name,
        String currency,
        String exchange,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal dayLow,
        BigDecimal dayHigh,
        BigDecimal fiftyTwoWeekLow,
        BigDecimal fiftyTwoWeekHigh,
        Long volume,
        BigDecimal marketCap,
        BigDecimal peRatio,
        BigDecimal dividendYield,
        LocalDate latestTradingDay,
        Instant fetchedAt,
        FinancialStatements financials
) {
}

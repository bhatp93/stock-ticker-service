package com.stock.ticker.provider;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.AlphaVantageException;
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.fundamentaldata.response.BalanceSheetResponse;
import com.crazzyghost.alphavantage.fundamentaldata.response.CashFlowResponse;
import com.crazzyghost.alphavantage.fundamentaldata.response.IncomeStatementResponse;
import com.crazzyghost.alphavantage.timeseries.response.QuoteResponse;
import com.stock.ticker.config.StockProperties;
import com.stock.ticker.model.FinancialStatements;
import com.stock.ticker.model.StockSnapshot;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Primary
public class AlphaVantageProvider implements StockDataProvider {

    private final String apiKey;
    private final StockProperties properties;
    private boolean initialized;

    public AlphaVantageProvider(StockProperties properties) {
        this.apiKey = properties.getAlphaVantageApiKey();
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            Config config = Config.builder()
                    .key(apiKey.trim())
                    .timeOut(15)
                    .build();
            AlphaVantage.api().init(config);
            initialized = true;
        }
    }

    @Override
    public String getProviderName() {
        return "alpha-vantage";
    }

    @Override
    public StockSnapshot fetch(String symbol) throws Exception {
        StockSnapshot quote = fetchQuote(symbol);
        FinancialStatements financials = fetchFinancialStatements(symbol);
        return new StockSnapshot(
                quote.symbol(),
                quote.name(),
                quote.currency(),
                quote.exchange(),
                quote.price(),
                quote.previousClose(),
                quote.dayLow(),
                quote.dayHigh(),
                quote.fiftyTwoWeekLow(),
                quote.fiftyTwoWeekHigh(),
                quote.volume(),
                quote.marketCap(),
                quote.peRatio(),
                quote.dividendYield(),
                quote.latestTradingDay(),
                quote.fetchedAt(),
                financials
        );
    }

    public StockSnapshot fetchQuote(String symbol) throws Exception {
        requireInitialized();
        QuoteResponse quote = AlphaVantage.api()
                .timeSeries()
                .quote()
                .forSymbol(symbol)
                .fetchSync();
        requireNoError(quote.getErrorMessage());

        return new StockSnapshot(
                quote.getSymbol(),
                null,
                null,
                null,
                toBigDecimal(quote.getPrice()),
                toBigDecimal(quote.getPreviousClose()),
                toBigDecimal(quote.getLow()),
                toBigDecimal(quote.getHigh()),
                null,
                null,
                (long) quote.getVolume(),
                null,
                null,
                null,
                parseTradingDay(quote.getLatestTradingDay()),
                Instant.now(),
                null
        );
    }

    public FinancialStatements fetchFinancialStatements(String symbol) throws Exception {
        requireInitialized();

        BalanceSheetResponse balanceSheet = AlphaVantage.api()
                .fundamentalData()
                .balanceSheet()
                .forSymbol(symbol)
                .fetchSync();
        requireNoError(balanceSheet.getErrorMessage());

        sleepBetweenApiCalls();

        IncomeStatementResponse incomeStatement = AlphaVantage.api()
                .fundamentalData()
                .incomeStatement()
                .forSymbol(symbol)
                .fetchSync();
        requireNoError(incomeStatement.getErrorMessage());

        sleepBetweenApiCalls();

        CashFlowResponse cashFlow = AlphaVantage.api()
                .fundamentalData()
                .cashFlow()
                .forSymbol(symbol)
                .fetchSync();
        requireNoError(cashFlow.getErrorMessage());

        return AlphaVantageFundamentalMapper.toFinancialStatements(
                symbol,
                balanceSheet,
                incomeStatement,
                cashFlow
        );
    }

    private void sleepBetweenApiCalls() {
        long delayMs = properties.getScheduling().getIngestionDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Alpha Vantage API key is not configured");
        }
    }

    private static void requireNoError(String errorMessage) {
        if (errorMessage != null) {
            throw new AlphaVantageException(errorMessage);
        }
    }

    private static BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    private static LocalDate parseTradingDay(String tradingDay) {
        if (tradingDay == null || tradingDay.isBlank()) {
            return null;
        }
        return LocalDate.parse(tradingDay.trim());
    }
}

package com.stock.ticker.ingestion;

import com.crazzyghost.alphavantage.AlphaVantageException;
import com.stock.ticker.model.StockSnapshot;
import com.stock.ticker.provider.AlphaVantageProvider;
import com.stock.ticker.provider.YahooFinanceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuoteIngestionService {

    private static final Logger log = LoggerFactory.getLogger(QuoteIngestionService.class);

    private final AlphaVantageProvider alphaVantageProvider;
    private final YahooFinanceProvider yahooFinanceProvider;

    public QuoteIngestionService(
            AlphaVantageProvider alphaVantageProvider,
            YahooFinanceProvider yahooFinanceProvider
    ) {
        this.alphaVantageProvider = alphaVantageProvider;
        this.yahooFinanceProvider = yahooFinanceProvider;
    }

    public StockSnapshot fetchQuote(String symbol) throws Exception {
        try {
            return alphaVantageProvider.fetchQuote(symbol);
        } catch (Exception ex) {
            if (!shouldFallback(ex)) {
                throw ex;
            }
            log.warn("Alpha Vantage quote failed for {} ({}), falling back to Yahoo Finance",
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

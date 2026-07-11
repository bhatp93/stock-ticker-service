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
    //private final YahooFinanceProvider yahooFinanceProvider;
    private final AlphaVantageProvider alphaVantageProvider;

    public QuoteIngestionService(
            //YahooFinanceProvider yahooFinanceProvider
            AlphaVantageProvider alphaVantageProvider

    ) {
        //this.yahooFinanceProvider = yahooFinanceProvider;
        this.alphaVantageProvider = alphaVantageProvider;
    }

    public StockSnapshot fetchQuote(String symbol) throws Exception {
        StockSnapshot alphaVantageQuote = null;
        try {
            alphaVantageQuote = alphaVantageProvider.fetchQuote(symbol);
        } catch (Exception ex) {
                log.warn("Alpha Vantage quote failed for {} ({})",
                        symbol, ex.getMessage());
                throw ex;
            //return yahooFinanceProvider.fetch(symbol);
        }
        return alphaVantageQuote;
    }

//    private static boolean shouldFallback(Exception ex) {
//        if (ex instanceof IllegalStateException || ex instanceof AlphaVantageException) {
//            return true;
//        }
//        String message = ex.getMessage();
//        if (message == null) {
//            return false;
//        }
//        String lower = message.toLowerCase();
//        return lower.contains("rate limit")
//                || lower.contains("api call frequency")
//                || lower.contains("thank you for using alpha vantage")
//                || lower.contains("premium");
//    }
}

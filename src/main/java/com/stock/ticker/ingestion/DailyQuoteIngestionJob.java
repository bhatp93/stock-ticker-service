package com.stock.ticker.ingestion;

import com.stock.ticker.config.StockProperties;
import com.stock.ticker.model.StockSnapshot;
import com.stock.ticker.service.TickerFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "stock.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DailyQuoteIngestionJob {

    private static final Logger log = LoggerFactory.getLogger(DailyQuoteIngestionJob.class);

    private final TickerFileService tickerFileService;
    private final QuoteIngestionService quoteIngestionService;
    private final IngestionPersistenceService persistenceService;
    private final long ingestionDelayMs;

    public DailyQuoteIngestionJob(
            TickerFileService tickerFileService,
            QuoteIngestionService quoteIngestionService,
            IngestionPersistenceService persistenceService,
            StockProperties properties
    ) {
        this.tickerFileService = tickerFileService;
        this.quoteIngestionService = quoteIngestionService;
        this.persistenceService = persistenceService;
        this.ingestionDelayMs = properties.getScheduling().getIngestionDelayMs();
    }

    @Scheduled(cron = "${stock.scheduling.daily-quote-cron}", zone = "UTC")
    public void run() {
        List<String> symbols;
        try {
            symbols = tickerFileService.readTickers();
        } catch (Exception ex) {
            log.error("Daily quote job failed to read tickers.txt", ex);
            return;
        }

        if (symbols.isEmpty()) {
            log.info("Daily quote job skipped: no symbols in tickers.txt");
            return;
        }

        log.info("Starting daily quote ingestion for {} symbol(s)", symbols.size());
        long runId = persistenceService.startRun(IngestionJobType.DAILY_QUOTE, symbols.size());

        int successCount = 0;
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            try {
                StockSnapshot snapshot = quoteIngestionService.fetchQuote(symbol);
                persistenceService.ensureSymbol(
                        snapshot.symbol(),
                        snapshot.name(),
                        snapshot.currency()
                );
                persistenceService.saveQuoteSnapshot(runId, snapshot);
                successCount++;
                log.debug("Saved quote for {}", symbol);
            } catch (Exception ex) {
                failures.add(symbol + ": " + ex.getMessage());
                log.warn("Failed to ingest quote for {}: {}", symbol, ex.getMessage());
            }

            if (i < symbols.size() - 1) {
                sleepBetweenSymbols();
            }
        }

        finishRun(runId, symbols.size(), successCount, failures);
    }

    private void finishRun(long runId, int total, int successCount, List<String> failures) {
        IngestionStatus status;
        if (successCount == total) {
            status = IngestionStatus.SUCCESS;
        } else if (successCount == 0) {
            status = IngestionStatus.FAILED;
        } else {
            status = IngestionStatus.PARTIAL;
        }

        String errorMessage = failures.isEmpty() ? null : String.join("; ", failures);
        persistenceService.completeRun(runId, status, truncate(errorMessage, 4000));
        log.info("Daily quote ingestion finished: {}/{} succeeded, status={}", successCount, total, status);
    }

    private void sleepBetweenSymbols() {
        if (ingestionDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(ingestionDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

package com.stock.ticker.service;

import com.stock.ticker.config.StockProperties;
import com.stock.ticker.model.StockSnapshot;
import com.stock.ticker.provider.StockDataProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockDataService {

    private final TickerFileService tickerFileService;
    private final StockDataProvider stockDataProvider;
    private final Duration cacheTtl;
    private final Map<String, CachedSnapshot> cache = new ConcurrentHashMap<>();

    public StockDataService(
            TickerFileService tickerFileService,
            StockDataProvider stockDataProvider,
            StockProperties properties
    ) {
        this.tickerFileService = tickerFileService;
        this.stockDataProvider = stockDataProvider;
        this.cacheTtl = Duration.ofSeconds(Math.max(0, properties.getCacheTtlSeconds()));
    }

    public StockSnapshot fetchOne(String symbol, boolean useCache) throws Exception {
        String normalized = symbol.trim().toUpperCase();
        if (useCache && cacheTtl.toSeconds() > 0) {
            CachedSnapshot cached = cache.get(normalized);
            if (cached != null && !cached.isExpired()) {
                return cached.snapshot();
            }
        }

        StockSnapshot snapshot = stockDataProvider.fetch(normalized);
        if (cacheTtl.toSeconds() > 0) {
            cache.put(normalized, new CachedSnapshot(snapshot, Instant.now().plus(cacheTtl)));
        }
        return snapshot;
    }

    public List<StockSnapshot> fetchFromFile(boolean useCache) throws Exception {
        List<String> tickers = tickerFileService.readTickers();
        List<StockSnapshot> results = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (String ticker : tickers) {
            try {
                results.add(fetchOne(ticker, useCache));
            } catch (Exception ex) {
                failures.add(ticker + ": " + ex.getMessage());
            }
        }

        if (!failures.isEmpty() && results.isEmpty()) {
            throw new IllegalStateException("Failed to fetch all tickers: " + String.join("; ", failures));
        }

        return results;
    }

    public void evictCache() {
        cache.clear();
    }

    public String getProviderName() {
        return stockDataProvider.getProviderName();
    }

    private record CachedSnapshot(StockSnapshot snapshot, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}

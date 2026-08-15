package com.stock.ticker.service;

import com.stock.ticker.config.StockProperties;
import com.stock.ticker.extraction.ExtractionService;
import com.stock.ticker.model.StockSnapshot;
import com.stock.ticker.model.WCRatioResponse;
import com.stock.ticker.model.WorkingCapital;
import com.stock.ticker.model.SalesToWC;
import com.stock.ticker.provider.StockDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockDataService {

    private final TickerFileService tickerFileService;
    private final StockDataProvider stockDataProvider;
    private static final Logger log = LoggerFactory.getLogger(StockDataService.class);
    private final ExtractionService extractionService;
    private final Duration cacheTtl;
    private final Map<String, CachedSnapshot> cache = new ConcurrentHashMap<>();

    public StockDataService(
            TickerFileService tickerFileService,
            StockDataProvider stockDataProvider,
            StockProperties properties,
            ExtractionService extractionService
    ) {
        this.tickerFileService = tickerFileService;
        this.stockDataProvider = stockDataProvider;
        this.extractionService = extractionService;
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

    public boolean isSymbolActive(String symbol){
        HashMap<String, Boolean> activeSymbols = extractionService.getActiveSymbols();
        return activeSymbols.containsKey(symbol);
    }

    public HashMap<Boolean, String> isValidSymbol(String symbol) {
        HashMap<Boolean, String> activeSymbolMap= new HashMap<>();
        try{
            List<String> tickers = tickerFileService.readTickers();
            boolean foundTickerInFile = false;
            for(String ticker : tickers){
                if(symbol.equals(ticker)){
                    foundTickerInFile = true;
                    break;
                }
            }
            if(!foundTickerInFile){
                HashMap<Boolean, String> returnValue = new HashMap<>();
                returnValue.put(false, "Symbol not found in ticker file");
                return returnValue;
            }
            if(!isSymbolActive(symbol)){
                HashMap<Boolean, String> returnValue = new HashMap<>();
                returnValue.put(false, "Symbol is Inactive");
                return returnValue;
            }
             activeSymbolMap.put(true, "Symbol is Active");
        }
        catch(Exception ex){
            log.error("Daily quote job failed to read tickers.txt and validate symbol", ex);
        }

        return activeSymbolMap;
    }

    public WCRatioResponse getWorkingCapitalRatioData(String symbol, String periodType, int limit){
        List<SalesToWC> salesToWCList = new ArrayList<>();
        List<WorkingCapital> dataForWorkingCapital = extractionService.getDataForWorkingCapital(symbol, periodType, limit);
        List<HashMap<String, String>> totalRevenue = extractionService.getTotalRevenue(symbol, periodType, limit);
        //List<HashMap<String, Long>> workingCapitalList = new ArrayList<>();
        if(dataForWorkingCapital.size() != totalRevenue.size())
            log.error("Working Capital and totalRevenue lists do not have the same size");
        long netReceivablesDifference = 0L;
        long inventoryDifference = 0L;
        long accountPayableDifference = 0L;
        long workingCapitalDifference = 0L;
        Double workingCapitalRatioDifference = 0D;
        long previousWorkingCapital = 0L;
        Double previousWorkingCapitalRatio = 0D;
        long revenueDifference = 0L;
        long previousRevenue = 0L;
        for(int i = dataForWorkingCapital.size() - 1; i>=0;  i--){
            WorkingCapital currentWorkingCapital = dataForWorkingCapital.get(i);
            WorkingCapital nextWorkingCapital = i>0 ? dataForWorkingCapital.get(i-1) : null;
            HashMap<String, String> revenueMap = totalRevenue.get(i);

            LocalDate totalRevenueFiscalDate = LocalDate.parse(revenueMap.get("fiscalDateEnding"));
            if(currentWorkingCapital.fiscalDateEnding().isEqual(totalRevenueFiscalDate)){
                Long workingCapitalCalculated = currentWorkingCapital.currentNetReceivables() + currentWorkingCapital.inventory() - currentWorkingCapital.currentAccountsPayable();
                Long totalRevenueCalculated =  Long.parseLong(revenueMap.get("totalRevenue"));
                Double workingCapitalRatioCalculated = (double)totalRevenueCalculated / workingCapitalCalculated;
                if(previousWorkingCapitalRatio != 0D){
                    workingCapitalRatioDifference = workingCapitalRatioCalculated - previousWorkingCapitalRatio;
                    workingCapitalDifference = workingCapitalCalculated - previousWorkingCapital;
                    revenueDifference = totalRevenueCalculated - previousRevenue;
                }
                previousWorkingCapitalRatio = workingCapitalRatioCalculated;
                previousWorkingCapital = workingCapitalCalculated;
                previousRevenue = totalRevenueCalculated;

                SalesToWC salesToWC = SalesToWC.getSalesToWC(currentWorkingCapital, workingCapitalCalculated, totalRevenueCalculated, workingCapitalRatioCalculated, workingCapitalDifference,
                        workingCapitalRatioDifference, netReceivablesDifference, inventoryDifference, accountPayableDifference, revenueDifference);
                salesToWCList.add(salesToWC);

                if(nextWorkingCapital != null){
                    netReceivablesDifference =  nextWorkingCapital.currentNetReceivables() - currentWorkingCapital.currentNetReceivables() ;
                    inventoryDifference = nextWorkingCapital.inventory() - currentWorkingCapital.inventory();
                    accountPayableDifference = nextWorkingCapital.currentAccountsPayable() - currentWorkingCapital.currentAccountsPayable();
                }

            }
            else
                log.error("Revenue entry not found for date " + currentWorkingCapital.fiscalDateEnding());
        }

        return WCRatioResponse.getWCRatioResponse(salesToWCList);
    }

}

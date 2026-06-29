package com.stock.ticker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock")
public class StockProperties {

    private String tickerFile = "tickers.txt";
    private int cacheTtlSeconds = 60;
    private String alphaVantageApiKey = "";
    private Scheduling scheduling = new Scheduling();

    public String getTickerFile() {
        return tickerFile;
    }

    public void setTickerFile(String tickerFile) {
        this.tickerFile = tickerFile;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getAlphaVantageApiKey() {
        return alphaVantageApiKey;
    }

    public void setAlphaVantageApiKey(String alphaVantageApiKey) {
        this.alphaVantageApiKey = alphaVantageApiKey;
    }

    public Scheduling getScheduling() {
        return scheduling;
    }

    public void setScheduling(Scheduling scheduling) {
        this.scheduling = scheduling;
    }

    public static class Scheduling {

        private boolean enabled = true;
        private String dailyQuoteCron = "0 0 22 * * MON-FRI";
        private String quarterlyFinancialsCron = "0 0 3 15 1,4,7,10 *";
        private long ingestionDelayMs = 13_000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDailyQuoteCron() {
            return dailyQuoteCron;
        }

        public void setDailyQuoteCron(String dailyQuoteCron) {
            this.dailyQuoteCron = dailyQuoteCron;
        }

        public String getQuarterlyFinancialsCron() {
            return quarterlyFinancialsCron;
        }

        public void setQuarterlyFinancialsCron(String quarterlyFinancialsCron) {
            this.quarterlyFinancialsCron = quarterlyFinancialsCron;
        }

        public long getIngestionDelayMs() {
            return ingestionDelayMs;
        }

        public void setIngestionDelayMs(long ingestionDelayMs) {
            this.ingestionDelayMs = ingestionDelayMs;
        }
    }
}

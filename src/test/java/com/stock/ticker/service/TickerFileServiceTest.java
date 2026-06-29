package com.stock.ticker.service;

import com.stock.ticker.config.StockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TickerFileServiceTest {

    @TempDir
    Path tempDir;

    private TickerFileService tickerFileService;

    @BeforeEach
    void setUp() {
        StockProperties properties = new StockProperties();
        properties.setTickerFile(tempDir.resolve("tickers.txt").toString());
        tickerFileService = new TickerFileService(properties);
    }

    @Test
    void readsIgnoresCommentsAndBlankLines() throws Exception {
        tickerFileService.writeTickers(List.of("AAPL", "MSFT"));
        assertThat(tickerFileService.readTickers()).containsExactly("AAPL", "MSFT");
    }

    @Test
    void addAndRemoveTicker() throws Exception {
        tickerFileService.addTicker("aapl");
        tickerFileService.addTicker("msft");
        assertThat(tickerFileService.readTickers()).containsExactly("AAPL", "MSFT");

        assertThat(tickerFileService.removeTicker("AAPL")).isTrue();
        assertThat(tickerFileService.readTickers()).containsExactly("MSFT");
    }
}

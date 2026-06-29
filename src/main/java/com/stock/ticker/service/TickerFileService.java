package com.stock.ticker.service;

import com.stock.ticker.config.StockProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TickerFileService {

    private final Path tickerFile;

    public TickerFileService(StockProperties properties) {
        this.tickerFile = Path.of(properties.getTickerFile()).toAbsolutePath().normalize();
    }

    public Path getTickerFilePath() {
        return tickerFile;
    }

    public List<String> readTickers() throws IOException {
        ensureFileExists();
        return Files.readAllLines(tickerFile, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void writeTickers(List<String> tickers) throws IOException {
        ensureParentDirectory();
        List<String> normalized = tickers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        String content = String.join(System.lineSeparator(), normalized);
        if (!content.isEmpty()) {
            content = content + System.lineSeparator();
        }
        Files.writeString(tickerFile, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public boolean addTicker(String ticker) throws IOException {
        String normalized = normalizeTicker(ticker);
        Set<String> tickers = new LinkedHashSet<>(readTickers());
        boolean added = tickers.add(normalized);
        if (added) {
            writeTickers(new ArrayList<>(tickers));
        }
        return added;
    }

    public boolean removeTicker(String ticker) throws IOException {
        String normalized = normalizeTicker(ticker);
        List<String> tickers = readTickers();
        boolean removed = tickers.removeIf(t -> t.equalsIgnoreCase(normalized));
        if (removed) {
            writeTickers(tickers);
        }
        return removed;
    }

    public void replaceTickers(List<String> tickers) throws IOException {
        writeTickers(tickers);
    }

    private void ensureFileExists() throws IOException {
        if (!Files.exists(tickerFile)) {
            ensureParentDirectory();
            Files.createFile(tickerFile);
        }
    }

    private void ensureParentDirectory() throws IOException {
        Path parent = tickerFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        return ticker.trim().toUpperCase();
    }
}

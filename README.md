# Stock Ticker Service

Spring Boot app that reads stock symbols from a text file (one per line), fetches market data from Yahoo Finance (similar to Python's `yfinance`), and exposes REST APIs for ticker management and downstream analysis.

## Prerequisites

- Java 17+
- Maven 3.9+

## Quick start

```bash
cd stock-ticker-service
mvn spring-boot:run
```

Edit `tickers.txt` in the project root (or set `stock.ticker-file` in `application.yml`):

```
AAPL
MSFT
GOOGL
```

Lines starting with `#` are comments. Blank lines are ignored.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tickers` | List tickers from the file |
| POST | `/api/tickers` | Add a ticker (`{"ticker":"NVDA"}`) |
| DELETE | `/api/tickers/{symbol}` | Remove a ticker |
| PUT | `/api/tickers` | Replace entire list (`{"tickers":["AAPL","TSLA"]}`) |
| GET | `/api/stocks` | Fetch data for all tickers in the file |
| GET | `/api/stocks/{symbol}` | Fetch one symbol |
| GET | `/api/stocks/analyze` | Fetch file tickers and run registered stock logics |
| POST | `/api/stocks/cache/evict` | Clear in-memory quote cache |

## Configuration (`application.yml`)

```yaml
stock:
  ticker-file: ${user.dir}/tickers.txt
  cache-ttl-seconds: 60
```

## Adding your own stock logic

Implement `StockLogic` and register it as a Spring `@Component`. See `PriceAbovePreviousCloseLogic` for an example. All registered logics run when you call `GET /api/stocks/analyze`.

Later you can wire add/remove rules into `TickerFileService` (scheduled jobs, filters on price/volume, etc.) without changing the data-fetch layer.

## Project layout

```
src/main/java/com/stock/ticker/
  service/          Ticker file + stock data orchestration
  provider/         Yahoo Finance adapter (swap providers here)
  logic/            Your analysis rules
  web/              REST controllers
```

## Notes

- Yahoo Finance is unofficial and rate-limited; the app caches quotes for 60 seconds by default.
- For production, consider a paid API (Polygon, Alpha Vantage, etc.) behind `StockDataProvider`.

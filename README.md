# Stock Ticker Service

Headless Spring Boot service that ingests daily stock quotes and quarterly financial statements from Alpha Vantage into PostgreSQL. Symbols are read from a watchlist file today; the schema already supports a database-managed registry with an `active` flag for future admin APIs and ratio endpoints.

## What it does today

- **Scheduled ingestion** (UTC cron):
  - **Daily quotes** — price, volume, market cap, P/E, etc.
  - **Quarterly financials** — income statement, balance sheet, and cash flow periods (annual + quarterly from Alpha Vantage)
- **Persistence** — Flyway migrations, JDBC upserts, ingestion run tracking
- **Data source** — Alpha Vantage (primary); Yahoo Finance as quote fallback via `FallbackStockDataProvider`
- **No HTTP API** — `web-application-type: none`; batch/scheduler only

## Planned direction

| Area | Status |
|------|--------|
| Symbol registry in PostgreSQL (`symbols.active`) | Schema ready; jobs still read `tickers.txt` |
| Admin API to add/remove/toggle active symbols | Planned |
| Read API for basic financial ratios (margins, liquidity, leverage) | Planned |
| Deploy from GHCR on a home server | Supported via Docker + compose |

---

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose (recommended)
- [Alpha Vantage API key](https://www.alphavantage.co/support/#api-key) (free tier is rate-limited)

---

## Quick start (Docker)

### 1. Environment

```bash
cp .env.example .env
```

Edit `.env` — at minimum set `POSTGRES_PASSWORD` and `ALPHA_VANTAGE_API_KEY`.

### 2. Watchlist

Edit `tickers.txt` in the project root (one symbol per line; `#` comments and blank lines are ignored):

```
AAPL
MSFT
GOOGL
```

### 3. Run the stack

```bash
docker compose up --build -d
docker compose logs -f stock-ticker-service
```

This starts:

- **postgres** — `postgres:16-alpine` on port `5432` (configurable via `POSTGRES_PORT`)
- **stock-ticker-service** — multi-stage Docker build (Maven + JRE 17)

The app connects to Postgres at `postgres:5432` inside the Compose network. `tickers.txt` is bind-mounted at `/app/tickers.txt` so you can edit symbols without rebuilding the image.

### 4. Stop

```bash
docker compose down          # keep database volume
docker compose down -v       # ⚠️ deletes Postgres data
```

---

## Local development (without Docker for the app)

Start Postgres only:

```bash
docker compose up -d postgres
```

Run the app on the host (uses `localhost:5432` — adjust `spring.datasource.url` in `application.yml` if needed):

```bash
export ALPHA_VANTAGE_API_KEY=your_key
mvn spring-boot:run
```

---

## Configuration

### Environment variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `POSTGRES_DB` | Database name | `stockdb` |
| `POSTGRES_USER` | Database user | `stock` |
| `POSTGRES_PASSWORD` / `POSTGRES_PWD` | Database password | — |
| `POSTGRES_PORT` | Host port for Postgres (compose) | `5432` |
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API key | — |
| `STOCK_DAILY_QUOTE_CRON` | Spring cron for daily quotes | `0 0 22 * * MON-FRI` |
| `STOCK_QUARTERLY_FINANCIALS_CRON` | Spring cron for financials | `0 0 3 15 1,4,7,10 *` |
| `STOCK_INGESTION_DELAY_MS` | Delay between API calls per symbol | `13000` |
| `APP_PORT` | Host port mapped to container `8080` | `8080` (unused until REST is added) |

Cron expressions use **6 fields** (seconds included). All scheduled jobs run in **UTC**.

Quote cron examples in `.env` (use quotes because of spaces):

```env
STOCK_DAILY_QUOTE_CRON="0 */5 * * * *"
```

### Application settings (`application.yml`)

Key paths under `stock.*`:

- `ticker-file` — watchlist path (`${user.dir}/tickers.txt` in Docker → `/app/tickers.txt`)
- `scheduling.enabled` — toggle scheduled jobs
- `scheduling.ingestion-delay-ms` — pause between symbols (Alpha Vantage free tier ~5 calls/min)

---

## Database schema

Flyway migrations live in `src/main/resources/db/migration/`.

| Table | Purpose |
|-------|---------|
| `symbols` | Symbol registry (`active` flag for future gating) |
| `ingestion_runs` | Job run history and status |
| `quote_snapshots` | Daily quote pulls |
| `income_statement_periods` | Income statement data |
| `balance_sheet_periods` | Balance sheet data |
| `cash_flow_periods` | Cash flow data |

---

## Container image (GHCR)

Images are built and pushed manually from the `main` branch via GitHub Actions:

**Workflow:** `.github/workflows/BuildTagPushImage.yml`  
**Registry:** `ghcr.io/bhatp93/stock-ticker-service`

1. GitHub → **Actions** → **build-tag-push-image** → **Run workflow**
2. Select branch `main` and enter a tag (e.g. `1.0`)
3. Publishes `:1.0` and `:latest`

Make the package **public** (or `docker login ghcr.io` on the server) under GitHub **Packages** → package settings.

### Deploy on a server with a pre-built image

On the server, use `docker-compose.yml` but replace local build with the GHCR image:

```yaml
stock-ticker-service:
  image: ghcr.io/bhatp93/stock-ticker-service:1.0
  # build: .   ← remove when pulling from GHCR
```

You still need on the server:

- `docker-compose.yml`
- `.env` (secrets — never commit)
- `tickers.txt` (until symbol management moves to the database)

```bash
docker compose pull
docker compose up -d
```

---

## Project layout

```
src/main/java/com/stock/ticker/
  config/           StockProperties, scheduling
  ingestion/        Cron jobs, persistence, ingestion runs
  model/            Domain records (quotes, financial periods)
  provider/         Alpha Vantage, Yahoo fallback, mappers
  service/          Ticker file + stock data orchestration

src/main/resources/
  application.yml
  db/migration/     Flyway SQL

.github/workflows/
  BuildTagPushImage.yml

Dockerfile          Multi-stage build (Maven → JRE)
docker-compose.yml  Postgres + app
```

---

## Notes

- **Alpha Vantage free tier** — ~5 API calls/minute; `ingestion-delay-ms` defaults to 13s between symbols.
- **`tickers.txt` is interim** — ingestion jobs read the file; `symbols.active` in the DB is written but not yet used to filter jobs.
- **Secrets** — `.env` is gitignored; do not commit API keys or database passwords.
- **Postgres image** — pulled from Docker Hub (`postgres:16-alpine`); only the app image is published to GHCR.

## License

Private / personal project — add a license if you open-source the repo.

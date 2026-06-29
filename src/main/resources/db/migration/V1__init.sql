CREATE TYPE period_type AS ENUM ('ANNUAL', 'QUARTERLY');

CREATE TYPE ingestion_job_type AS ENUM ('DAILY_QUOTE', 'QUARTERLY_FINANCIALS');

CREATE TYPE ingestion_status AS ENUM ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED');

CREATE TABLE symbols (
    symbol     VARCHAR(16)  PRIMARY KEY,
    name       VARCHAR(255),
    currency   VARCHAR(8),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE ingestion_runs (
    id             BIGSERIAL PRIMARY KEY,
    job_type       ingestion_job_type NOT NULL,
    started_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at    TIMESTAMPTZ,
    status         ingestion_status NOT NULL DEFAULT 'RUNNING',
    symbols_count  INT,
    error_message  TEXT
);

CREATE TABLE quote_snapshots (
    id                   BIGSERIAL PRIMARY KEY,
    symbol               VARCHAR(16)  NOT NULL REFERENCES symbols (symbol),
    pulled_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ingestion_run_id     BIGINT REFERENCES ingestion_runs (id),
    price                NUMERIC(18, 6),
    previous_close       NUMERIC(18, 6),
    day_low              NUMERIC(18, 6),
    day_high             NUMERIC(18, 6),
    fifty_two_week_low   NUMERIC(18, 6),
    fifty_two_week_high  NUMERIC(18, 6),
    volume               BIGINT,
    market_cap           NUMERIC(20, 2),
    pe_ratio             NUMERIC(12, 4),
    dividend_yield       NUMERIC(8, 4),
    latest_trading_day   DATE,
    CONSTRAINT uq_quote_snapshot_per_pull UNIQUE (symbol, pulled_at)
);

CREATE INDEX idx_quote_snapshots_symbol_pulled_at
    ON quote_snapshots (symbol, pulled_at DESC);

CREATE TABLE balance_sheet_periods (
    id                              BIGSERIAL PRIMARY KEY,
    symbol                          VARCHAR(16) NOT NULL REFERENCES symbols (symbol),
    fiscal_date_ending              DATE        NOT NULL,
    period_type                     period_type NOT NULL,
    reported_currency               VARCHAR(8),
    pulled_at                       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ingestion_run_id                BIGINT REFERENCES ingestion_runs (id),
    total_assets                    BIGINT,
    total_current_assets            BIGINT,
    cash_and_cash_equivalents       BIGINT,
    total_liabilities               BIGINT,
    total_current_liabilities       BIGINT,
    long_term_debt                  BIGINT,
    total_shareholder_equity        BIGINT,
    retained_earnings               BIGINT,
    common_stock_shares_outstanding BIGINT,
    CONSTRAINT uq_balance_sheet_period UNIQUE (symbol, fiscal_date_ending, period_type)
);

CREATE INDEX idx_balance_sheet_symbol_fiscal
    ON balance_sheet_periods (symbol, fiscal_date_ending DESC);

CREATE TABLE income_statement_periods (
    id                       BIGSERIAL PRIMARY KEY,
    symbol                   VARCHAR(16) NOT NULL REFERENCES symbols (symbol),
    fiscal_date_ending       DATE        NOT NULL,
    period_type              period_type NOT NULL,
    reported_currency        VARCHAR(8),
    pulled_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ingestion_run_id         BIGINT REFERENCES ingestion_runs (id),
    total_revenue            BIGINT,
    gross_profit             BIGINT,
    operating_income         BIGINT,
    operating_expenses       BIGINT,
    research_and_development BIGINT,
    income_before_tax        BIGINT,
    income_tax_expense       BIGINT,
    net_income               BIGINT,
    ebitda                   BIGINT,
    CONSTRAINT uq_income_statement_period UNIQUE (symbol, fiscal_date_ending, period_type)
);

CREATE INDEX idx_income_statement_symbol_fiscal
    ON income_statement_periods (symbol, fiscal_date_ending DESC);

CREATE TABLE cash_flow_periods (
    id                             BIGSERIAL PRIMARY KEY,
    symbol                         VARCHAR(16) NOT NULL REFERENCES symbols (symbol),
    fiscal_date_ending             DATE        NOT NULL,
    period_type                    period_type NOT NULL,
    reported_currency              VARCHAR(8),
    pulled_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ingestion_run_id               BIGINT REFERENCES ingestion_runs (id),
    operating_cashflow             BIGINT,
    capital_expenditures           BIGINT,
    cashflow_from_investment       BIGINT,
    cashflow_from_financing        BIGINT,
    dividend_payout                BIGINT,
    change_in_cash_and_equivalents BIGINT,
    net_income                     BIGINT,
    CONSTRAINT uq_cash_flow_period UNIQUE (symbol, fiscal_date_ending, period_type)
);

CREATE INDEX idx_cash_flow_symbol_fiscal
    ON cash_flow_periods (symbol, fiscal_date_ending DESC);

package com.stock.ticker.ingestion;

import com.stock.ticker.model.BalanceSheetPeriod;
import com.stock.ticker.model.CashFlowPeriod;
import com.stock.ticker.model.FinancialStatements;
import com.stock.ticker.model.IncomeStatementPeriod;
import com.stock.ticker.model.StockSnapshot;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class IngestionPersistenceService {

    private final NamedParameterJdbcTemplate jdbc;

    public IngestionPersistenceService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void ensureSymbol(String symbol, String name, String currency) {
        jdbc.update("""
                INSERT INTO symbols (symbol, name, currency, active)
                VALUES (:symbol, :name, :currency, TRUE)
                ON CONFLICT (symbol) DO UPDATE SET
                    name = COALESCE(EXCLUDED.name, symbols.name),
                    currency = COALESCE(EXCLUDED.currency, symbols.currency),
                    active = TRUE
                """,
                new MapSqlParameterSource()
                        .addValue("symbol", symbol)
                        .addValue("name", name)
                        .addValue("currency", currency)
        );
    }

    @Transactional
    public long startRun(IngestionJobType jobType, int symbolsCount) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO ingestion_runs (job_type, status, symbols_count)
                VALUES (:jobType::ingestion_job_type, 'RUNNING'::ingestion_status, :symbolsCount)
                """,
                new MapSqlParameterSource()
                        .addValue("jobType", jobType.name())
                        .addValue("symbolsCount", symbolsCount),
                keyHolder,
                new String[]{"id"}
        );
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Transactional
    public void completeRun(long runId, IngestionStatus status, String errorMessage) {
        jdbc.update("""
                UPDATE ingestion_runs
                SET finished_at = NOW(),
                    status = :status::ingestion_status,
                    error_message = :errorMessage
                WHERE id = :runId
                """,
                new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("status", status.name())
                        .addValue("errorMessage", errorMessage)
        );
    }

    @Transactional
    public void saveQuoteSnapshot(long runId, StockSnapshot snapshot) {
        Instant pulledAt = snapshot.fetchedAt() != null ? snapshot.fetchedAt() : Instant.now();
        jdbc.update("""
                INSERT INTO quote_snapshots (
                    symbol, pulled_at, ingestion_run_id,
                    price, previous_close, day_low, day_high,
                    fifty_two_week_low, fifty_two_week_high,
                    volume, market_cap, pe_ratio, dividend_yield, latest_trading_day
                ) VALUES (
                    :symbol, :pulledAt, :runId,
                    :price, :previousClose, :dayLow, :dayHigh,
                    :fiftyTwoWeekLow, :fiftyTwoWeekHigh,
                    :volume, :marketCap, :peRatio, :dividendYield, :latestTradingDay
                )
                """,
                quoteParams(runId, snapshot, pulledAt)
        );
    }

    @Transactional
    public void upsertFinancials(long runId, FinancialStatements financials) {
        if (financials == null) {
            return;
        }
        Instant pulledAt = Instant.now();
        for (BalanceSheetPeriod period : financials.balanceSheetAnnual()) {
            upsertBalanceSheet(runId, financials.symbol(), period, PeriodType.ANNUAL, pulledAt);
        }
        for (BalanceSheetPeriod period : financials.balanceSheetQuarterly()) {
            upsertBalanceSheet(runId, financials.symbol(), period, PeriodType.QUARTERLY, pulledAt);
        }
        for (IncomeStatementPeriod period : financials.incomeStatementAnnual()) {
            upsertIncomeStatement(runId, financials.symbol(), period, PeriodType.ANNUAL, pulledAt);
        }
        for (IncomeStatementPeriod period : financials.incomeStatementQuarterly()) {
            upsertIncomeStatement(runId, financials.symbol(), period, PeriodType.QUARTERLY, pulledAt);
        }
        for (CashFlowPeriod period : financials.cashFlowAnnual()) {
            upsertCashFlow(runId, financials.symbol(), period, PeriodType.ANNUAL, pulledAt);
        }
        for (CashFlowPeriod period : financials.cashFlowQuarterly()) {
            upsertCashFlow(runId, financials.symbol(), period, PeriodType.QUARTERLY, pulledAt);
        }
    }

    private void upsertBalanceSheet(
            long runId,
            String symbol,
            BalanceSheetPeriod period,
            PeriodType periodType,
            Instant pulledAt
    ) {
        jdbc.update("""
                INSERT INTO balance_sheet_periods (
                    symbol, fiscal_date_ending, period_type, reported_currency, pulled_at, ingestion_run_id,
                    total_assets, total_current_assets, cash_and_cash_equivalents,
                    cash_and_short_term_investments, inventory, current_net_receivables,
                    total_non_current_assets, property_plant_equipment, accumulated_depreciation_amortization_ppe,
                    intangible_assets, intangible_assets_excluding_goodwill, goodwill, investments,
                    long_term_investments, short_term_investments, other_current_assets, other_non_current_assets,
                    total_liabilities, total_current_liabilities, current_accounts_payable, deferred_revenue,
                    current_debt, short_term_debt, total_non_current_liabilities, capital_lease_obligations,
                    long_term_debt, current_long_term_debt, long_term_debt_noncurrent, short_long_term_debt_total,
                    other_current_liabilities, other_non_current_liabilities,
                    total_shareholder_equity, treasury_stock, retained_earnings, common_stock,
                    common_stock_shares_outstanding
                ) VALUES (
                    :symbol, :fiscalDateEnding, :periodType::period_type, :reportedCurrency, :pulledAt, :runId,
                    :totalAssets, :totalCurrentAssets, :cashAndCashEquivalents,
                    :cashAndShortTermInvestments, :inventory, :currentNetReceivables,
                    :totalNonCurrentAssets, :propertyPlantEquipment, :accumulatedDepreciationAmortizationPpe,
                    :intangibleAssets, :intangibleAssetsExcludingGoodwill, :goodwill, :investments,
                    :longTermInvestments, :shortTermInvestments, :otherCurrentAssets, :otherNonCurrentAssets,
                    :totalLiabilities, :totalCurrentLiabilities, :currentAccountsPayable, :deferredRevenue,
                    :currentDebt, :shortTermDebt, :totalNonCurrentLiabilities, :capitalLeaseObligations,
                    :longTermDebt, :currentLongTermDebt, :longTermDebtNoncurrent, :shortLongTermDebtTotal,
                    :otherCurrentLiabilities, :otherNonCurrentLiabilities,
                    :totalShareholderEquity, :treasuryStock, :retainedEarnings, :commonStock,
                    :commonStockSharesOutstanding
                )
                ON CONFLICT (symbol, fiscal_date_ending, period_type) DO UPDATE SET
                    reported_currency = EXCLUDED.reported_currency,
                    pulled_at = EXCLUDED.pulled_at,
                    ingestion_run_id = EXCLUDED.ingestion_run_id,
                    total_assets = EXCLUDED.total_assets,
                    total_current_assets = EXCLUDED.total_current_assets,
                    cash_and_cash_equivalents = EXCLUDED.cash_and_cash_equivalents,
                    cash_and_short_term_investments = EXCLUDED.cash_and_short_term_investments,
                    inventory = EXCLUDED.inventory,
                    current_net_receivables = EXCLUDED.current_net_receivables,
                    total_non_current_assets = EXCLUDED.total_non_current_assets,
                    property_plant_equipment = EXCLUDED.property_plant_equipment,
                    accumulated_depreciation_amortization_ppe = EXCLUDED.accumulated_depreciation_amortization_ppe,
                    intangible_assets = EXCLUDED.intangible_assets,
                    intangible_assets_excluding_goodwill = EXCLUDED.intangible_assets_excluding_goodwill,
                    goodwill = EXCLUDED.goodwill,
                    investments = EXCLUDED.investments,
                    long_term_investments = EXCLUDED.long_term_investments,
                    short_term_investments = EXCLUDED.short_term_investments,
                    other_current_assets = EXCLUDED.other_current_assets,
                    other_non_current_assets = EXCLUDED.other_non_current_assets,
                    total_liabilities = EXCLUDED.total_liabilities,
                    total_current_liabilities = EXCLUDED.total_current_liabilities,
                    current_accounts_payable = EXCLUDED.current_accounts_payable,
                    deferred_revenue = EXCLUDED.deferred_revenue,
                    current_debt = EXCLUDED.current_debt,
                    short_term_debt = EXCLUDED.short_term_debt,
                    total_non_current_liabilities = EXCLUDED.total_non_current_liabilities,
                    capital_lease_obligations = EXCLUDED.capital_lease_obligations,
                    long_term_debt = EXCLUDED.long_term_debt,
                    current_long_term_debt = EXCLUDED.current_long_term_debt,
                    long_term_debt_noncurrent = EXCLUDED.long_term_debt_noncurrent,
                    short_long_term_debt_total = EXCLUDED.short_long_term_debt_total,
                    other_current_liabilities = EXCLUDED.other_current_liabilities,
                    other_non_current_liabilities = EXCLUDED.other_non_current_liabilities,
                    total_shareholder_equity = EXCLUDED.total_shareholder_equity,
                    treasury_stock = EXCLUDED.treasury_stock,
                    retained_earnings = EXCLUDED.retained_earnings,
                    common_stock = EXCLUDED.common_stock,
                    common_stock_shares_outstanding = EXCLUDED.common_stock_shares_outstanding
                """,
                new MapSqlParameterSource()
                        .addValue("symbol", symbol)
                        .addValue("fiscalDateEnding", parseFiscalDate(period.fiscalDateEnding()))
                        .addValue("periodType", periodType.name())
                        .addValue("reportedCurrency", period.reportedCurrency())
                        .addValue("pulledAt", Timestamp.from(pulledAt))
                        .addValue("runId", runId)
                        .addValue("totalAssets", period.totalAssets(), Types.BIGINT)
                        .addValue("totalCurrentAssets", period.totalCurrentAssets(), Types.BIGINT)
                        .addValue("cashAndCashEquivalents", period.cashAndCashEquivalentsAtCarryingValue(), Types.BIGINT)
                        .addValue("cashAndShortTermInvestments", period.cashAndShortTermInvestments(), Types.BIGINT)
                        .addValue("inventory", period.inventory(), Types.BIGINT)
                        .addValue("currentNetReceivables", period.currentNetReceivables(), Types.BIGINT)
                        .addValue("totalNonCurrentAssets", period.totalNonCurrentAssets(), Types.BIGINT)
                        .addValue("propertyPlantEquipment", period.propertyPlantEquipment(), Types.BIGINT)
                        .addValue("accumulatedDepreciationAmortizationPpe", period.accumulatedDepreciationAmortizationPPE(), Types.BIGINT)
                        .addValue("intangibleAssets", period.intangibleAssets(), Types.BIGINT)
                        .addValue("intangibleAssetsExcludingGoodwill", period.intangibleAssetsExcludingGoodwill(), Types.BIGINT)
                        .addValue("goodwill", period.goodwill(), Types.BIGINT)
                        .addValue("investments", period.investments(), Types.BIGINT)
                        .addValue("longTermInvestments", period.longTermInvestments(), Types.BIGINT)
                        .addValue("shortTermInvestments", period.shortTermInvestments(), Types.BIGINT)
                        .addValue("otherCurrentAssets", period.otherCurrentAssets(), Types.BIGINT)
                        .addValue("otherNonCurrentAssets", period.otherNonCurrentAssets(), Types.BIGINT)
                        .addValue("totalLiabilities", period.totalLiabilities(), Types.BIGINT)
                        .addValue("totalCurrentLiabilities", period.totalCurrentLiabilities(), Types.BIGINT)
                        .addValue("currentAccountsPayable", period.currentAccountsPayable(), Types.BIGINT)
                        .addValue("deferredRevenue", period.deferredRevenue(), Types.BIGINT)
                        .addValue("currentDebt", period.currentDebt(), Types.BIGINT)
                        .addValue("shortTermDebt", period.shortTermDebt(), Types.BIGINT)
                        .addValue("totalNonCurrentLiabilities", period.totalNonCurrentLiabilities(), Types.BIGINT)
                        .addValue("capitalLeaseObligations", period.capitalLeaseObligations(), Types.BIGINT)
                        .addValue("longTermDebt", period.longTermDebt(), Types.BIGINT)
                        .addValue("currentLongTermDebt", period.currentLongTermDebt(), Types.BIGINT)
                        .addValue("longTermDebtNoncurrent", period.longTermDebtNonCurrent(), Types.BIGINT)
                        .addValue("shortLongTermDebtTotal", period.shortLongTermDebtTotal(), Types.BIGINT)
                        .addValue("otherCurrentLiabilities", period.otherCurrentLiabilities(), Types.BIGINT)
                        .addValue("otherNonCurrentLiabilities", period.otherNonCurrentLiabilities(), Types.BIGINT)
                        .addValue("totalShareholderEquity", period.totalShareholderEquity(), Types.BIGINT)
                        .addValue("treasuryStock", period.treasuryStock(), Types.BIGINT)
                        .addValue("retainedEarnings", period.retainedEarnings(), Types.BIGINT)
                        .addValue("commonStock", period.commonStock(), Types.BIGINT)
                        .addValue("commonStockSharesOutstanding", period.commonStockSharesOutstanding(), Types.BIGINT)
        );
    }

    private void upsertIncomeStatement(
            long runId,
            String symbol,
            IncomeStatementPeriod period,
            PeriodType periodType,
            Instant pulledAt
    ) {
        jdbc.update("""
                INSERT INTO income_statement_periods (
                    symbol, fiscal_date_ending, period_type, reported_currency, pulled_at, ingestion_run_id,
                    total_revenue, gross_profit, cost_of_revenue, cost_of_goods_and_services_sold,
                    operating_income, selling_general_and_administrative, research_and_development,
                    operating_expenses, investment_income_net, net_interest_income, interest_income,
                    interest_expense, non_interest_income, other_non_operating_income, depreciation,
                    depreciation_and_amortization, income_before_tax, income_tax_expense,
                    interest_and_debt_expense, net_income_from_continuing_operations,
                    comprehensive_income_net_of_tax, ebit, ebitda, net_income
                ) VALUES (
                    :symbol, :fiscalDateEnding, :periodType::period_type, :reportedCurrency, :pulledAt, :runId,
                    :totalRevenue, :grossProfit, :costOfRevenue, :costOfGoodsAndServicesSold,
                    :operatingIncome, :sellingGeneralAndAdministrative, :researchAndDevelopment,
                    :operatingExpenses, :investmentIncomeNet, :netInterestIncome, :interestIncome,
                    :interestExpense, :nonInterestIncome, :otherNonOperatingIncome, :depreciation,
                    :depreciationAndAmortization, :incomeBeforeTax, :incomeTaxExpense,
                    :interestAndDebtExpense, :netIncomeFromContinuingOperations,
                    :comprehensiveIncomeNetOfTax, :ebit, :ebitda, :netIncome
                )
                ON CONFLICT (symbol, fiscal_date_ending, period_type) DO UPDATE SET
                    reported_currency = EXCLUDED.reported_currency,
                    pulled_at = EXCLUDED.pulled_at,
                    ingestion_run_id = EXCLUDED.ingestion_run_id,
                    total_revenue = EXCLUDED.total_revenue,
                    gross_profit = EXCLUDED.gross_profit,
                    cost_of_revenue = EXCLUDED.cost_of_revenue,
                    cost_of_goods_and_services_sold = EXCLUDED.cost_of_goods_and_services_sold,
                    operating_income = EXCLUDED.operating_income,
                    selling_general_and_administrative = EXCLUDED.selling_general_and_administrative,
                    research_and_development = EXCLUDED.research_and_development,
                    operating_expenses = EXCLUDED.operating_expenses,
                    investment_income_net = EXCLUDED.investment_income_net,
                    net_interest_income = EXCLUDED.net_interest_income,
                    interest_income = EXCLUDED.interest_income,
                    interest_expense = EXCLUDED.interest_expense,
                    non_interest_income = EXCLUDED.non_interest_income,
                    other_non_operating_income = EXCLUDED.other_non_operating_income,
                    depreciation = EXCLUDED.depreciation,
                    depreciation_and_amortization = EXCLUDED.depreciation_and_amortization,
                    income_before_tax = EXCLUDED.income_before_tax,
                    income_tax_expense = EXCLUDED.income_tax_expense,
                    interest_and_debt_expense = EXCLUDED.interest_and_debt_expense,
                    net_income_from_continuing_operations = EXCLUDED.net_income_from_continuing_operations,
                    comprehensive_income_net_of_tax = EXCLUDED.comprehensive_income_net_of_tax,
                    ebit = EXCLUDED.ebit,
                    ebitda = EXCLUDED.ebitda,
                    net_income = EXCLUDED.net_income
                """,
                new MapSqlParameterSource()
                        .addValue("symbol", symbol)
                        .addValue("fiscalDateEnding", parseFiscalDate(period.fiscalDateEnding()))
                        .addValue("periodType", periodType.name())
                        .addValue("reportedCurrency", period.reportedCurrency())
                        .addValue("pulledAt", Timestamp.from(pulledAt))
                        .addValue("runId", runId)
                        .addValue("totalRevenue", period.totalRevenue(), Types.BIGINT)
                        .addValue("grossProfit", period.grossProfit(), Types.BIGINT)
                        .addValue("costOfRevenue", period.costOfRevenue(), Types.BIGINT)
                        .addValue("costOfGoodsAndServicesSold", period.costofGoodsAndServicesSold(), Types.BIGINT)
                        .addValue("operatingIncome", period.operatingIncome(), Types.BIGINT)
                        .addValue("sellingGeneralAndAdministrative", period.sellingGeneralAndAdministrative(), Types.BIGINT)
                        .addValue("researchAndDevelopment", period.researchAndDevelopment(), Types.BIGINT)
                        .addValue("operatingExpenses", period.operatingExpenses(), Types.BIGINT)
                        .addValue("investmentIncomeNet", period.investmentIncomeNet(), Types.BIGINT)
                        .addValue("netInterestIncome", period.netInterestIncome(), Types.BIGINT)
                        .addValue("interestIncome", period.interestIncome(), Types.BIGINT)
                        .addValue("interestExpense", period.interestExpense(), Types.BIGINT)
                        .addValue("nonInterestIncome", period.nonInterestIncome(), Types.BIGINT)
                        .addValue("otherNonOperatingIncome", period.otherNonOperatingIncome(), Types.BIGINT)
                        .addValue("depreciation", period.depreciation(), Types.BIGINT)
                        .addValue("depreciationAndAmortization", period.depreciationAndAmortization(), Types.BIGINT)
                        .addValue("incomeBeforeTax", period.incomeBeforeTax(), Types.BIGINT)
                        .addValue("incomeTaxExpense", period.incomeTaxExpense(), Types.BIGINT)
                        .addValue("interestAndDebtExpense", period.interestAndDebtExpense(), Types.BIGINT)
                        .addValue("netIncomeFromContinuingOperations", period.netIncomeFromContinuingOperations(), Types.BIGINT)
                        .addValue("comprehensiveIncomeNetOfTax", period.comprehensiveIncomeNetOfTax(), Types.BIGINT)
                        .addValue("ebit", period.ebit(), Types.BIGINT)
                        .addValue("ebitda", period.ebitda(), Types.BIGINT)
                        .addValue("netIncome", period.netIncome(), Types.BIGINT)
        );
    }

    private void upsertCashFlow(
            long runId,
            String symbol,
            CashFlowPeriod period,
            PeriodType periodType,
            Instant pulledAt
    ) {
        jdbc.update("""
                INSERT INTO cash_flow_periods (
                    symbol, fiscal_date_ending, period_type, reported_currency, pulled_at, ingestion_run_id,
                    operating_cashflow, payments_for_operating_activities, proceeds_from_operating_activities,
                    change_in_operating_liabilities, change_in_operating_assets,
                    depreciation_depletion_and_amortization, capital_expenditures, change_in_receivables,
                    change_in_inventory, profit_loss, cashflow_from_investment, cashflow_from_financing,
                    proceeds_from_repayments_of_short_term_debt, payments_for_repurchase_of_common_stock,
                    payments_for_repurchase_of_equity, payments_for_repurchase_of_preferred_stock,
                    dividend_payout, dividend_payout_common_stock, dividend_payout_preferred_stock,
                    proceeds_from_issuance_of_common_stock,
                    proceeds_from_issuance_of_long_term_debt_and_capital_securities_net,
                    proceeds_from_issuance_of_preferred_stock, proceeds_from_repurchase_of_equity,
                    proceeds_from_sale_of_treasury_stock, stock_based_compensation,
                    change_in_cash_and_equivalents, change_in_exchange_rate, net_income
                ) VALUES (
                    :symbol, :fiscalDateEnding, :periodType::period_type, :reportedCurrency, :pulledAt, :runId,
                    :operatingCashflow, :paymentsForOperatingActivities, :proceedsFromOperatingActivities,
                    :changeInOperatingLiabilities, :changeInOperatingAssets,
                    :depreciationDepletionAndAmortization, :capitalExpenditures, :changeInReceivables,
                    :changeInInventory, :profitLoss, :cashflowFromInvestment, :cashflowFromFinancing,
                    :proceedsFromRepaymentsOfShortTermDebt, :paymentsForRepurchaseOfCommonStock,
                    :paymentsForRepurchaseOfEquity, :paymentsForRepurchaseOfPreferredStock,
                    :dividendPayout, :dividendPayoutCommonStock, :dividendPayoutPreferredStock,
                    :proceedsFromIssuanceOfCommonStock,
                    :proceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet,
                    :proceedsFromIssuanceOfPreferredStock, :proceedsFromRepurchaseOfEquity,
                    :proceedsFromSaleOfTreasuryStock, :stockBasedCompensation,
                    :changeInCashAndEquivalents, :changeInExchangeRate, :netIncome
                )
                ON CONFLICT (symbol, fiscal_date_ending, period_type) DO UPDATE SET
                    reported_currency = EXCLUDED.reported_currency,
                    pulled_at = EXCLUDED.pulled_at,
                    ingestion_run_id = EXCLUDED.ingestion_run_id,
                    operating_cashflow = EXCLUDED.operating_cashflow,
                    payments_for_operating_activities = EXCLUDED.payments_for_operating_activities,
                    proceeds_from_operating_activities = EXCLUDED.proceeds_from_operating_activities,
                    change_in_operating_liabilities = EXCLUDED.change_in_operating_liabilities,
                    change_in_operating_assets = EXCLUDED.change_in_operating_assets,
                    depreciation_depletion_and_amortization = EXCLUDED.depreciation_depletion_and_amortization,
                    capital_expenditures = EXCLUDED.capital_expenditures,
                    change_in_receivables = EXCLUDED.change_in_receivables,
                    change_in_inventory = EXCLUDED.change_in_inventory,
                    profit_loss = EXCLUDED.profit_loss,
                    cashflow_from_investment = EXCLUDED.cashflow_from_investment,
                    cashflow_from_financing = EXCLUDED.cashflow_from_financing,
                    proceeds_from_repayments_of_short_term_debt = EXCLUDED.proceeds_from_repayments_of_short_term_debt,
                    payments_for_repurchase_of_common_stock = EXCLUDED.payments_for_repurchase_of_common_stock,
                    payments_for_repurchase_of_equity = EXCLUDED.payments_for_repurchase_of_equity,
                    payments_for_repurchase_of_preferred_stock = EXCLUDED.payments_for_repurchase_of_preferred_stock,
                    dividend_payout = EXCLUDED.dividend_payout,
                    dividend_payout_common_stock = EXCLUDED.dividend_payout_common_stock,
                    dividend_payout_preferred_stock = EXCLUDED.dividend_payout_preferred_stock,
                    proceeds_from_issuance_of_common_stock = EXCLUDED.proceeds_from_issuance_of_common_stock,
                    proceeds_from_issuance_of_long_term_debt_and_capital_securities_net = EXCLUDED.proceeds_from_issuance_of_long_term_debt_and_capital_securities_net,
                    proceeds_from_issuance_of_preferred_stock = EXCLUDED.proceeds_from_issuance_of_preferred_stock,
                    proceeds_from_repurchase_of_equity = EXCLUDED.proceeds_from_repurchase_of_equity,
                    proceeds_from_sale_of_treasury_stock = EXCLUDED.proceeds_from_sale_of_treasury_stock,
                    stock_based_compensation = EXCLUDED.stock_based_compensation,
                    change_in_cash_and_equivalents = EXCLUDED.change_in_cash_and_equivalents,
                    change_in_exchange_rate = EXCLUDED.change_in_exchange_rate,
                    net_income = EXCLUDED.net_income
                """,
                new MapSqlParameterSource()
                        .addValue("symbol", symbol)
                        .addValue("fiscalDateEnding", parseFiscalDate(period.fiscalDateEnding()))
                        .addValue("periodType", periodType.name())
                        .addValue("reportedCurrency", period.reportedCurrency())
                        .addValue("pulledAt", Timestamp.from(pulledAt))
                        .addValue("runId", runId)
                        .addValue("operatingCashflow", period.operatingCashflow(), Types.BIGINT)
                        .addValue("paymentsForOperatingActivities", period.paymentsForOperatingActivities(), Types.BIGINT)
                        .addValue("proceedsFromOperatingActivities", period.proceedsFromOperatingActivities(), Types.BIGINT)
                        .addValue("changeInOperatingLiabilities", period.changeInOperatingLiabilities(), Types.BIGINT)
                        .addValue("changeInOperatingAssets", period.changeInOperatingAssets(), Types.BIGINT)
                        .addValue("depreciationDepletionAndAmortization", period.depreciationDepletionAndAmortization(), Types.BIGINT)
                        .addValue("capitalExpenditures", period.capitalExpenditures(), Types.BIGINT)
                        .addValue("changeInReceivables", period.changeInReceivables(), Types.BIGINT)
                        .addValue("changeInInventory", period.changeInInventory(), Types.BIGINT)
                        .addValue("profitLoss", period.profitLoss(), Types.BIGINT)
                        .addValue("cashflowFromInvestment", period.cashflowFromInvestment(), Types.BIGINT)
                        .addValue("cashflowFromFinancing", period.cashflowFromFinancing(), Types.BIGINT)
                        .addValue("proceedsFromRepaymentsOfShortTermDebt", period.proceedsFromRepaymentsOfShortTermDebt(), Types.BIGINT)
                        .addValue("paymentsForRepurchaseOfCommonStock", period.paymentsForRepurchaseOfCommonStock(), Types.BIGINT)
                        .addValue("paymentsForRepurchaseOfEquity", period.paymentsForRepurchaseOfEquity(), Types.BIGINT)
                        .addValue("paymentsForRepurchaseOfPreferredStock", period.paymentsForRepurchaseOfPreferredStock(), Types.BIGINT)
                        .addValue("dividendPayout", period.dividendPayout(), Types.BIGINT)
                        .addValue("dividendPayoutCommonStock", period.dividendPayoutCommonStock(), Types.BIGINT)
                        .addValue("dividendPayoutPreferredStock", period.dividendPayoutPreferredStock(), Types.BIGINT)
                        .addValue("proceedsFromIssuanceOfCommonStock", period.proceedsFromIssuanceOfCommonStock(), Types.BIGINT)
                        .addValue("proceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet", period.proceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet(), Types.BIGINT)
                        .addValue("proceedsFromIssuanceOfPreferredStock", period.proceedsFromIssuanceOfPreferredStock(), Types.BIGINT)
                        .addValue("proceedsFromRepurchaseOfEquity", period.proceedsFromRepurchaseOfEquity(), Types.BIGINT)
                        .addValue("proceedsFromSaleOfTreasuryStock", period.proceedsFromSaleOfTreasuryStock(), Types.BIGINT)
                        .addValue("stockBasedCompensation", period.stockBasedCompensation(), Types.BIGINT)
                        .addValue("changeInCashAndEquivalents", period.changeInCashAndCashEquivalents(), Types.BIGINT)
                        .addValue("changeInExchangeRate", period.changeInExchangeRate(), Types.BIGINT)
                        .addValue("netIncome", period.netIncome(), Types.BIGINT)
        );
    }

    private static MapSqlParameterSource quoteParams(long runId, StockSnapshot snapshot, Instant pulledAt) {
        return new MapSqlParameterSource()
                .addValue("symbol", snapshot.symbol())
                .addValue("pulledAt", Timestamp.from(pulledAt))
                .addValue("runId", runId)
                .addValue("price", snapshot.price())
                .addValue("previousClose", snapshot.previousClose())
                .addValue("dayLow", snapshot.dayLow())
                .addValue("dayHigh", snapshot.dayHigh())
                .addValue("fiftyTwoWeekLow", snapshot.fiftyTwoWeekLow())
                .addValue("fiftyTwoWeekHigh", snapshot.fiftyTwoWeekHigh())
                .addValue("volume", snapshot.volume(), Types.BIGINT)
                .addValue("marketCap", snapshot.marketCap())
                .addValue("peRatio", snapshot.peRatio())
                .addValue("dividendYield", snapshot.dividendYield())
                .addValue("latestTradingDay", toSqlDate(snapshot.latestTradingDay()));
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static LocalDate parseFiscalDate(String fiscalDateEnding) {
        if (fiscalDateEnding == null || fiscalDateEnding.isBlank()) {
            throw new IllegalArgumentException("fiscal_date_ending is required");
        }
        return LocalDate.parse(fiscalDateEnding.trim());
    }
}

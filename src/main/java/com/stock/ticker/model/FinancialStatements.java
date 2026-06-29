package com.stock.ticker.model;

import java.util.List;

public record FinancialStatements(
        String symbol,
        List<BalanceSheetPeriod> balanceSheetAnnual,
        List<BalanceSheetPeriod> balanceSheetQuarterly,
        List<IncomeStatementPeriod> incomeStatementAnnual,
        List<IncomeStatementPeriod> incomeStatementQuarterly,
        List<CashFlowPeriod> cashFlowAnnual,
        List<CashFlowPeriod> cashFlowQuarterly
) {
}

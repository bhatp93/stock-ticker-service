package com.stock.ticker.model;

public record IncomeStatementPeriod(
        String fiscalDateEnding,
        String reportedCurrency,
        Long totalRevenue,
        Long grossProfit,
        Long costOfRevenue,
        Long costofGoodsAndServicesSold,
        Long operatingIncome,
        Long sellingGeneralAndAdministrative,
        Long researchAndDevelopment,
        Long operatingExpenses,
        Long investmentIncomeNet,
        Long netInterestIncome,
        Long interestIncome,
        Long interestExpense,
        Long nonInterestIncome,
        Long otherNonOperatingIncome,
        Long depreciation,
        Long depreciationAndAmortization,
        Long incomeBeforeTax,
        Long incomeTaxExpense,
        Long interestAndDebtExpense,
        Long netIncomeFromContinuingOperations,
        Long comprehensiveIncomeNetOfTax,
        Long ebit,
        Long ebitda,
        Long netIncome
) {
}

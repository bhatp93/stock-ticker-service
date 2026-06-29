package com.stock.ticker.provider;

import com.crazzyghost.alphavantage.fundamentaldata.response.BalanceSheet;
import com.crazzyghost.alphavantage.fundamentaldata.response.BalanceSheetResponse;
import com.crazzyghost.alphavantage.fundamentaldata.response.CashFlow;
import com.crazzyghost.alphavantage.fundamentaldata.response.CashFlowResponse;
import com.crazzyghost.alphavantage.fundamentaldata.response.IncomeStatement;
import com.crazzyghost.alphavantage.fundamentaldata.response.IncomeStatementResponse;
import com.stock.ticker.model.BalanceSheetPeriod;
import com.stock.ticker.model.CashFlowPeriod;
import com.stock.ticker.model.FinancialStatements;
import com.stock.ticker.model.IncomeStatementPeriod;

import java.util.List;

final class AlphaVantageFundamentalMapper {

    private AlphaVantageFundamentalMapper() {
    }

    static FinancialStatements toFinancialStatements(
            String symbol,
            BalanceSheetResponse balanceSheet,
            IncomeStatementResponse incomeStatement,
            CashFlowResponse cashFlow
    ) {
        return new FinancialStatements(
                symbol,
                mapBalanceSheets(balanceSheet.getAnnualReports()),
                mapBalanceSheets(balanceSheet.getQuarterlyReports()),
                mapIncomeStatements(incomeStatement.getAnnualReports()),
                mapIncomeStatements(incomeStatement.getQuarterlyReports()),
                mapCashFlows(cashFlow.getAnnualReports()),
                mapCashFlows(cashFlow.getQuarterlyReports())
        );
    }

    private static List<BalanceSheetPeriod> mapBalanceSheets(List<BalanceSheet> reports) {
        return reports.stream().map(AlphaVantageFundamentalMapper::mapBalanceSheet).toList();
    }

    private static BalanceSheetPeriod mapBalanceSheet(BalanceSheet report) {
        return new BalanceSheetPeriod(
                report.getFiscalDateEnding(),
                report.getReportedCurrency(),
                report.getTotalAssets(),
                report.getTotalCurrentAssets(),
                report.getCashAndCashEquivalentsAtCarryingValue(),
                report.getCashAndShortTermInvestments(),
                report.getInventory(),
                report.getCurrentNetReceivables(),
                report.getTotalNonCurrentAssets(),
                report.getPropertyPlantEquipment(),
                report.getAccumulatedDepreciationAmortizationPPE(),
                report.getIntangibleAssets(),
                report.getIntangibleAssetsExcludingGoodwill(),
                report.getGoodWill(),
                report.getInvestments(),
                report.getLongTermInvestments(),
                report.getShortTermInvestments(),
                report.getOtherCurrentAssets(),
                report.getOtherNonCurrentAssets(),
                report.getTotalLiabilities(),
                report.getTotalCurrentLiabilities(),
                report.getCurrentAccountsPayable(),
                report.getDeferredRevenue(),
                report.getCurrentDebt(),
                report.getShortTermDebt(),
                report.getTotalNonCurrentLiabilities(),
                report.getCapitalLeaseObligations(),
                report.getLongTermDebt(),
                report.getCurrentLongTermDebt(),
                report.getLongTermDebtNonCurrent(),
                report.getShortLongTermDebtTotal(),
                report.getOtherCurrentLiabilities(),
                report.getOtherNonCurrentLiabilities(),
                report.getTotalShareholderEquity(),
                report.getTreasuryStock(),
                report.getRetainedEarnings(),
                report.getCommonStock(),
                report.getCommonStockSharesOutstanding()
        );
    }

    private static List<IncomeStatementPeriod> mapIncomeStatements(List<IncomeStatement> reports) {
        return reports.stream().map(AlphaVantageFundamentalMapper::mapIncomeStatement).toList();
    }

    private static IncomeStatementPeriod mapIncomeStatement(IncomeStatement report) {
        return new IncomeStatementPeriod(
                report.getFiscalDateEnding(),
                report.getReportedCurrency(),
                report.getTotalRevenue(),
                report.getGrossProfit(),
                report.getCostOfRevenue(),
                report.getCostofGoodsAndServicesSold(),
                report.getOperatingIncome(),
                report.getSellingGeneralAndAdministrative(),
                report.getResearchAndDevelopment(),
                report.getOperatingExpenses(),
                report.getInvestmentIncomeNet(),
                report.getNetInterestIncome(),
                report.getInterestIncome(),
                report.getInterestExpense(),
                report.getNonInterestIncome(),
                report.getOtherNonOperatingIncome(),
                report.getDepreciation(),
                report.getDepreciationAndAmortization(),
                report.getIncomeBeforeTax(),
                report.getIncomeTaxExpense(),
                report.getInterestAndDebtExpense(),
                report.getNetIncomeFromContinuingOperations(),
                report.getComprehensiveIncomeNetOfTax(),
                report.getEbit(),
                report.getEbitda(),
                report.getNetIncome()
        );
    }

    private static List<CashFlowPeriod> mapCashFlows(List<CashFlow> reports) {
        return reports.stream().map(AlphaVantageFundamentalMapper::mapCashFlow).toList();
    }

    private static CashFlowPeriod mapCashFlow(CashFlow report) {
        return new CashFlowPeriod(
                report.getFiscalDateEnding(),
                report.getReportedCurrency(),
                report.getOperatingCashflow(),
                report.getPaymentsForOperatingActivities(),
                report.getProceedsFromOperatingActivities(),
                report.getChangeInOperatingLiabilities(),
                report.getChangeInOperatingAssets(),
                report.getDepreciationDepletionAndAmortization(),
                report.getCapitalExpenditures(),
                report.getChangeInReceivables(),
                report.getChangeInInventory(),
                report.getProfitLoss(),
                report.getCashflowFromInvestment(),
                report.getCashflowFromFinancing(),
                report.getProceedsFromRepaymentsOfShortTermDebt(),
                report.getPaymentsForRepurchaseOfCommonStock(),
                report.getPaymentsForRepurchaseOfEquity(),
                report.getPaymentsForRepurchaseOfPreferredStock(),
                report.getDividendPayout(),
                report.getDividendPayoutCommonStock(),
                report.getDividendPayoutPreferredStock(),
                report.getProceedsFromIssuanceOfCommonStock(),
                report.getProceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet(),
                report.getProceedsFromIssuanceOfPreferredStock(),
                report.getProceedsFromRepurchaseOfEquity(),
                report.getProceedsFromSaleOfTreasuryStock(),
                null,
                report.getChangeInCashAndCashEquivalents(),
                report.getChangeInExchangeRate(),
                report.getNetIncome()
        );
    }
}

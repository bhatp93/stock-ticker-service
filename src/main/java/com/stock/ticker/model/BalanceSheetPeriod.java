package com.stock.ticker.model;

public record BalanceSheetPeriod(
        String fiscalDateEnding,
        String reportedCurrency,
        Long totalAssets,
        Long totalCurrentAssets,
        Long cashAndCashEquivalentsAtCarryingValue,
        Long cashAndShortTermInvestments,
        Long inventory,
        Long currentNetReceivables,
        Long totalNonCurrentAssets,
        Long propertyPlantEquipment,
        Long accumulatedDepreciationAmortizationPPE,
        Long intangibleAssets,
        Long intangibleAssetsExcludingGoodwill,
        Long goodwill,
        Long investments,
        Long longTermInvestments,
        Long shortTermInvestments,
        Long otherCurrentAssets,
        Long otherNonCurrentAssets,
        Long totalLiabilities,
        Long totalCurrentLiabilities,
        Long currentAccountsPayable,
        Long deferredRevenue,
        Long currentDebt,
        Long shortTermDebt,
        Long totalNonCurrentLiabilities,
        Long capitalLeaseObligations,
        Long longTermDebt,
        Long currentLongTermDebt,
        Long longTermDebtNonCurrent,
        Long shortLongTermDebtTotal,
        Long otherCurrentLiabilities,
        Long otherNonCurrentLiabilities,
        Long totalShareholderEquity,
        Long treasuryStock,
        Long retainedEarnings,
        Long commonStock,
        Long commonStockSharesOutstanding
) {
    public static BalanceSheetPeriod forWorkingCapital(
            Long inventory,
            Long currentNetReceivables,
            Long currentAccountsPayable
    ) {
        return new BalanceSheetPeriod(
                null, null, null, null, null, null,
                inventory,
                currentNetReceivables,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null,
                currentAccountsPayable,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null
        );
    }
}

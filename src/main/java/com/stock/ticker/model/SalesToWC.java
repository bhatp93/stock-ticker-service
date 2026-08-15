package com.stock.ticker.model;

import java.time.LocalDate;

public record SalesToWC(
        String inventory,
        String currentNetReceivables,
        String currentAccountsPayable,
        LocalDate fiscalDateEnding,
        String totalWorkingCapital,
        String totalRevenue,
        String workingCapitalRatio,
        String workingCapitalDifference,
        String workingCapitalRatioDifference,
        String netReceivablesDifference,
        String inventoryLevelDifference,
        String netPayableDifference,

        String revenueDifference



) {
    public static SalesToWC getSalesToWC(WorkingCapital workingCapital, Long totalWorkingCapital, Long totalRevenue, Double workingCapitalRatio, Long workingCapitalDifference,
                                         Double workingCapitalRatioDifference, Long netReceivablesDifference, Long inventoryLevelDifference, Long netPayableDifference, Long revenueDifference){
        String accountsPayableString = convertToBillMillKill(workingCapital.currentAccountsPayable());
        String inventoryString = convertToBillMillKill(workingCapital.inventory());
        String accountReceivableString = convertToBillMillKill(workingCapital.currentNetReceivables());
        String totalWorkingCapitalString = convertToBillMillKill(totalWorkingCapital);
        String totalRevenueString = convertToBillMillKill(totalRevenue);
        String workingCapitalDifferenceString = convertToBillMillKill(workingCapitalDifference);
        String netReceivablesDifferenceString = convertToBillMillKill(netReceivablesDifference);
        String inventoryLevelDifferenceString = convertToBillMillKill(inventoryLevelDifference);
        String netPayableDifferenceString = convertToBillMillKill(netPayableDifference);
        String revenueDifferenceString = convertToBillMillKill(revenueDifference);
        String wcRatioString = String.format("%.2f", workingCapitalRatio);
        String wcRatioDiffString = String.format("%.2f", workingCapitalRatioDifference);


        return new SalesToWC(inventoryString, accountReceivableString,accountsPayableString, workingCapital.fiscalDateEnding(), totalWorkingCapitalString, totalRevenueString, wcRatioString,
                workingCapitalDifferenceString, wcRatioDiffString, netReceivablesDifferenceString, inventoryLevelDifferenceString, netPayableDifferenceString, revenueDifferenceString);
    }

    private static String convertToBillMillKill(Long number){
        boolean negativeNumber = number < 0 ? true : false;
        number = Math.abs(number);
        double value = (double) number /1000000000;
        String returnValue = value + "B";
        if(value > 1)
            return negativeNumber ? "-"+returnValue : returnValue;
        value = (double) number/1000000;
        returnValue = value + "M";
        if(value > 1)
            return negativeNumber ? "-"+returnValue : returnValue;
        value = (double) number/1000;
        returnValue = value + "K";
        return negativeNumber ? "-"+returnValue : returnValue;
    }
}

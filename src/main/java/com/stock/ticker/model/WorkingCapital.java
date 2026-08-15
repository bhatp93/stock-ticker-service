package com.stock.ticker.model;

import java.time.LocalDate;

public record WorkingCapital(
        Long inventory,
        Long currentNetReceivables,
        Long currentAccountsPayable,
        LocalDate fiscalDateEnding
        ) {
    public static WorkingCapital forWorkingCapitalRatio(Long inventory, Long currentNetReceivables, Long currentAccountsPayable, String fiscalDateEndingDB){
        LocalDate fiscalDateEnding = LocalDate.parse(fiscalDateEndingDB);
        return new WorkingCapital(inventory, currentNetReceivables, currentAccountsPayable, fiscalDateEnding);
    }
}

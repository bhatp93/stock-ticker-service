package com.stock.ticker.controller;


import com.stock.ticker.errorHandling.BadRequestException;

import com.stock.ticker.ingestion.PeriodType;
import com.stock.ticker.model.WCRatioResponse;

import com.stock.ticker.service.StockDataService;
import com.stock.ticker.service.TickerFileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping(value = "/fundamentalData")
public class FundamentalDataController {
    private static final Logger log = LoggerFactory.getLogger(FundamentalDataController.class);
    private final TickerFileService tickerFileService;
    private final StockDataService stockDataService;
    FundamentalDataController(TickerFileService tickerFileService, StockDataService stockDataService){
        this.tickerFileService = tickerFileService;
        this.stockDataService = stockDataService;
    }
    @GetMapping("/basicFormula")
    public List<String> basicFormula() {
        return new ArrayList<>() {
            {
                add("Sales Revenue - Cost of Goods Sold = Gross Profit");
                add("Gross Profit - Operating Expenses = Operating Income");
                add("Operating Income - Interest Expense - Income Taxes = Net Income");
            }};
    }

    @GetMapping("/wcRatioData")
    public ResponseEntity<WCRatioResponse> getWorkingCapitalRatio(@RequestParam String symbol, @RequestParam String periodType, @RequestParam int dataLimit){


        HashMap<Boolean, String> validSymbol = stockDataService.isValidSymbol(symbol);
        if(validSymbol.containsKey(false)){
            throw new BadRequestException(validSymbol.get(false));
        }
        try{
            PeriodType.valueOf(periodType.toUpperCase());
        }
        catch(IllegalArgumentException e){
            throw new BadRequestException("Invalid periodType. Allowed values: ANNUAL, QUARTERLY");
        }
        if(dataLimit < 1 || dataLimit > 10 )
            throw new BadRequestException("the data limit is historical data size. Cannot be less than 1 and greater than 10");

        WCRatioResponse workingCapitalRatioData = stockDataService.getWorkingCapitalRatioData(symbol, periodType, dataLimit);
        return ResponseEntity.ok().body(workingCapitalRatioData);
    }
}

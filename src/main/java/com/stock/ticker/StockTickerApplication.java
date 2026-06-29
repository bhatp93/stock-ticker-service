package com.stock.ticker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StockTickerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTickerApplication.class, args);
    }
}

package com.stock.ticker.errorHandling;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
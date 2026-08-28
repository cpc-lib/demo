package com.example.limitdemo;

public class ConcurrencyLimitException extends RuntimeException {

    public ConcurrencyLimitException(String message) {
        super(message);
    }
}

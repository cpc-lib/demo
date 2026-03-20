package com.example.limit.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("限流");
    }
}

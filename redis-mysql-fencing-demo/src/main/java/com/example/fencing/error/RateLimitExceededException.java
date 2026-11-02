package com.example.fencing.error;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String key) {
        super("Rate limit exceeded for key: " + key);
    }
}

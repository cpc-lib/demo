package com.example.fencing.error;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String requestId) {
        super("Idempotency key reused with a different delta: " + requestId);
    }
}

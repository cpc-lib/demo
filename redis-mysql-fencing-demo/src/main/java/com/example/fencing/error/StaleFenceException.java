package com.example.fencing.error;

public class StaleFenceException extends RuntimeException {
    public StaleFenceException(String resourceId, long expectedToken) {
        super("Stale fencing token " + expectedToken + " for resource " + resourceId);
    }
}

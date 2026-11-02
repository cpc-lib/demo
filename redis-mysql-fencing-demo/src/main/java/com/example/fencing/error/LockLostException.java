package com.example.fencing.error;

public class LockLostException extends RuntimeException {
    public LockLostException(String resourceId) {
        super("Redis lock ownership was lost for resource " + resourceId);
    }
}

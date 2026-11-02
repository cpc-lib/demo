package com.example.fencing.error;

public class LockInterruptedException extends RuntimeException {
    public LockInterruptedException(String resourceId, Throwable cause) {
        super("Interrupted while acquiring resource lock: " + resourceId, cause);
    }
}

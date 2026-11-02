package com.example.fencing.error;

public class LockBusyException extends RuntimeException {
    public LockBusyException(String resourceId) {
        super("Could not acquire resource lock: " + resourceId);
    }
}

package com.example.distributedlock.lock;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class DistributedLockExecutor {

    private final DistributedLockRegistry registry;

    public DistributedLockExecutor(DistributedLockRegistry registry) {
        this.registry = registry;
    }

    public <T> T execute(
            LockType type,
            String key,
            Duration waitTime,
            Duration leaseTime,
            Supplier<T> criticalSection) {

        DistributedLock distributedLock = registry.get(type);
        LockHandle handle = distributedLock.tryLock(key, waitTime, leaseTime)
                .orElseThrow(() -> new LockAcquisitionException(type, key));

        try (handle) {
            return criticalSection.get();
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(LockType type, String key) {
            super("Failed to acquire " + type + " distributed lock: " + key);
        }
    }
}

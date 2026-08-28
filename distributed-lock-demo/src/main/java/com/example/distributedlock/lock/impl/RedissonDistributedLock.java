package com.example.distributedlock.lock.impl;

import com.example.distributedlock.lock.DistributedLock;
import com.example.distributedlock.lock.LockHandle;
import com.example.distributedlock.lock.LockType;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RedissonDistributedLock implements DistributedLock {

    private static final String PREFIX = "distributed-lock-demo:";

    private final RedissonClient redissonClient;

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public LockType type() {
        return LockType.REDISSON;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime) {
        RLock lock = redissonClient.getLock(PREFIX + key);
        try {
            // This overload keeps Redisson's watchdog semantics instead of forcing a fixed lease time.
            boolean acquired = lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                return Optional.empty();
            }

            AtomicBoolean closed = new AtomicBoolean(false);
            return Optional.of(new LockHandle() {
                @Override
                public String key() {
                    return key;
                }

                @Override
                public LockType type() {
                    return LockType.REDISSON;
                }

                @Override
                public void close() {
                    if (closed.compareAndSet(false, true) && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}

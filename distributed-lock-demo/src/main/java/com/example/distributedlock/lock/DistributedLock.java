package com.example.distributedlock.lock;

import java.time.Duration;
import java.util.Optional;

public interface DistributedLock {

    LockType type();

    /**
     * @param key logical business lock key
     * @param waitTime maximum time to wait for the lock
     * @param leaseTime lease hint. MySQL uses it directly; Redisson uses its watchdog;
     *                  ZooKeeper uses session/ephemeral-node semantics.
     */
    Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime);
}

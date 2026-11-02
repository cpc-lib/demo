package com.example.fencing.service;

import com.example.fencing.error.LockBusyException;
import com.example.fencing.error.LockInterruptedException;
import com.example.fencing.error.LockLostException;
import com.example.fencing.model.MutationResponse;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RedisLockCoordinator {
    private static final Logger log = LoggerFactory.getLogger(RedisLockCoordinator.class);
    private final RedissonClient redisson;
    private final MySqlFenceIssuer fenceIssuer;
    private final CounterMutationService mutations;
    private final long waitSeconds;

    public RedisLockCoordinator(RedissonClient redisson, MySqlFenceIssuer fenceIssuer,
            CounterMutationService mutations,
            @Value("${app.lock.wait-seconds:2}") long waitSeconds) {
        this.redisson = redisson;
        this.fenceIssuer = fenceIssuer;
        this.mutations = mutations;
        this.waitSeconds = waitSeconds;
    }

    public MutationResponse add(String resourceId, String requestId, long delta) {
        // Hash tag makes all keys for this resource route to the same Redis Cluster slot.
        RLock lock = redisson.getLock("lock:counter:{" + resourceId + "}");
        boolean acquired;
        try {
            // No fixed leaseTime: Redisson Watchdog extends TTL while the lock is held.
            acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockInterruptedException(resourceId, e);
        }
        if (!acquired) {
            throw new LockBusyException(resourceId);
        }
        try {
            assertStillOwnLock(lock, resourceId);
            // Replay does not mutate data or waste a new fencing token.
            MutationResponse replay = mutations.replayIfExists(resourceId, requestId, delta);
            if (replay != null) {
                return replay;
            }
            assertStillOwnLock(lock, resourceId);
            // Independent committed MySQL transaction; invalidates earlier fencing tokens.
            long token = fenceIssuer.issue(resourceId);
            assertStillOwnLock(lock, resourceId);
            // Separate MySQL transaction: conditional business write + idempotency event.
            return mutations.apply(resourceId, requestId, delta, token);
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                } else {
                    log.warn("Lost Redis lock before unlock for resource {}", resourceId);
                }
            } catch (Exception unlockFailure) {
                // Never force-unlock, because another owner may now hold the lock.
                log.error("Redis lock release failed for resource {}", resourceId, unlockFailure);
            }
        }
    }

    private void assertStillOwnLock(RLock lock, String resourceId) {
        // This is only a best-effort Redis-side check, NOT an atomic cross-system proof.
        // Database conditional updates are the authoritative fence against stale tokens.
        if (!lock.isHeldByCurrentThread()) {
            throw new LockLostException(resourceId);
        }
    }
}

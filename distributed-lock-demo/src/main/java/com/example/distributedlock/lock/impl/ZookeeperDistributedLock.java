package com.example.distributedlock.lock.impl;

import com.example.distributedlock.lock.DistributedLock;
import com.example.distributedlock.lock.LockHandle;
import com.example.distributedlock.lock.LockType;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ZookeeperDistributedLock implements DistributedLock {

    private static final String ROOT = "/distributed-lock-demo/locks/";

    private final CuratorFramework curatorFramework;

    public ZookeeperDistributedLock(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public LockType type() {
        return LockType.ZOOKEEPER;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime) {
        String safeKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getBytes(StandardCharsets.UTF_8));
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, ROOT + safeKey);

        try {
            boolean acquired = mutex.acquire(waitTime.toMillis(), TimeUnit.MILLISECONDS);
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
                    return LockType.ZOOKEEPER;
                }

                @Override
                public void close() {
                    if (!closed.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        mutex.release();
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to release ZooKeeper lock: " + key, e);
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to acquire ZooKeeper lock: " + key, e);
        }
    }
}

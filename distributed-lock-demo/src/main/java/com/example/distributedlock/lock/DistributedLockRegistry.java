package com.example.distributedlock.lock;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DistributedLockRegistry {

    private final Map<LockType, DistributedLock> locks;

    public DistributedLockRegistry(List<DistributedLock> lockImplementations) {
        EnumMap<LockType, DistributedLock> map = new EnumMap<>(LockType.class);
        for (DistributedLock lock : lockImplementations) {
            DistributedLock previous = map.put(lock.type(), lock);
            if (previous != null) {
                throw new IllegalStateException("Duplicate lock implementation: " + lock.type());
            }
        }
        this.locks = Map.copyOf(map);
    }

    public DistributedLock get(LockType type) {
        DistributedLock lock = locks.get(type);
        if (lock == null) {
            throw new IllegalArgumentException("Unsupported lock provider: " + type);
        }
        return lock;
    }
}

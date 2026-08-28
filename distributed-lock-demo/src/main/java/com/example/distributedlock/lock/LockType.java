package com.example.distributedlock.lock;

public enum LockType {
    REDISSON,
    ZOOKEEPER,
    MYSQL;

    public static LockType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider cannot be blank");
        }
        return LockType.valueOf(value.trim().toUpperCase());
    }
}

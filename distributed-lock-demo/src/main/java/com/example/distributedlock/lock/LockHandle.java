package com.example.distributedlock.lock;

public interface LockHandle extends AutoCloseable {

    String key();

    LockType type();

    @Override
    void close();
}

package com.example.idgen.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class SegmentBuffer {

    private volatile Segment current;
    private volatile Segment next;
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService prefetchPool = Executors.newSingleThreadExecutor();

    public long nextId(SegmentSupplier supplier, int step) {
        if (current == null) {
            lock.lock();
            try {
                if (current == null) {
                    current = supplier.get();
                }
            } finally {
                lock.unlock();
            }
        }

        long id = current.next();
        if (id != -1) {
            if (next == null && current.remaining() < step * 0.2) {
                prefetchPool.submit(() -> next = supplier.get());
            }
            return id;
        }

        lock.lock();
        try {
            if (next == null) {
                next = supplier.get();
            }
            current = next;
            next = null;
            return current.next();
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface SegmentSupplier {
        Segment get();
    }
}

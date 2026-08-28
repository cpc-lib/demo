package com.example.idgen.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Double-buffered segment. Supports async prefetch.
 */
public class SegmentBuffer {
    private static final Logger log = LoggerFactory.getLogger(SegmentBuffer.class);

    private volatile Segment current;
    private volatile Segment next;

    private final ReentrantLock switchLock = new ReentrantLock();
    private final ExecutorService prefetchPool;
    private volatile Future<?> prefetchFuture;

    public SegmentBuffer(ExecutorService prefetchPool) {
        this.prefetchPool = prefetchPool;
    }

    public long nextSeq(SegmentSupplier supplier, double threshold, Duration waitForPrefetch) {
        Segment cur = current;
        if (cur == null) {
            switchLock.lock();
            try {
                if (current == null) {
                    current = supplier.get();
                    log.debug("Initial segment {}", current);
                }
                cur = current;
            } finally {
                switchLock.unlock();
            }
        }

        long v = cur.next();
        if (v != -1L) {
            maybePrefetch(cur, supplier, threshold);
            return v;
        }

        switchLock.lock();
        try {
            Segment c2 = current;
            long v2 = c2.next();
            if (v2 != -1L) {
                maybePrefetch(c2, supplier, threshold);
                return v2;
            }

            if (next == null) {
                Future<?> f = prefetchFuture;
                if (f != null) {
                    try {
                        f.get(waitForPrefetch.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                if (next == null) {
                    next = supplier.get();
                    log.debug("Sync fetched next segment {}", next);
                }
            }

            current = next;
            next = null;
            prefetchFuture = null;

            long out = current.next();
            if (out == -1L) {
                throw new IllegalStateException("Switched segment but still exhausted: " + current);
            }
            return out;
        } finally {
            switchLock.unlock();
        }
    }

    private void maybePrefetch(Segment cur, SegmentSupplier supplier, double threshold) {
        if (next != null) return;
        if (threshold <= 0 || threshold >= 1) threshold = 0.2d;

        long remain = cur.remaining();
        long trigger = (long) (cur.size() * threshold);
        if (remain <= trigger) {
            Future<?> f = prefetchFuture;
            if (f == null) {
                synchronized (this) {
                    if (prefetchFuture == null && next == null) {
                        prefetchFuture = prefetchPool.submit(() -> {
                            try {
                                next = supplier.get();
                                log.debug("Prefetched next segment {}", next);
                            } catch (Exception e) {
                                log.warn("Prefetch failed: {}", e.toString());
                            }
                        });
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface SegmentSupplier {
        Segment get();
    }
}

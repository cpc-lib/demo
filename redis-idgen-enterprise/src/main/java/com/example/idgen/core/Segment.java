package com.example.idgen.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sequence segment: [start, end]
 */
public final class Segment {
    private final long start;
    private final long end;
    private final AtomicLong cursor;

    public Segment(long start, long end) {
        if (end < start) {
            throw new IllegalArgumentException("end must be >= start");
        }
        this.start = start;
        this.end = end;
        this.cursor = new AtomicLong(start);
    }

    public long next() {
        long v = cursor.getAndIncrement();
        return v <= end ? v : -1L;
    }

    public long remaining() {
        return Math.max(0L, end - cursor.get() + 1);
    }

    public long size() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return "Segment{" + start + ".." + end + ", cur=" + cursor.get() + "}";
    }
}

package com.example.idgen.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds a few extra pre-allocated segments as emergency reserve.
 * Used when Redis is down (or slow) to keep serving IDs.
 */
public class EmergencyReserve {
    private final int capacity;
    private final Deque<Segment> deque = new ArrayDeque<>();

    public EmergencyReserve(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public synchronized void offer(Segment seg) {
        if (capacity == 0 || seg == null) return;
        while (deque.size() >= capacity) {
            deque.pollFirst();
        }
        deque.offerLast(seg);
    }

    public synchronized Segment poll() {
        return deque.pollLast();
    }

    public synchronized int size() {
        return deque.size();
    }
}

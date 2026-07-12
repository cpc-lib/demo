package com.example.orderclose.service;

public record OrderCloseResult(
        int scanned,
        int closed,
        int skipped,
        int rounds,
        boolean reachedLimit
) {
}

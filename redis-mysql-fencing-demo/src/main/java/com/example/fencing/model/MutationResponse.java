package com.example.fencing.model;

public record MutationResponse(
        String resourceId,
        String idempotencyKey,
        long delta,
        long value,
        long fenceToken,
        boolean replayed) { }

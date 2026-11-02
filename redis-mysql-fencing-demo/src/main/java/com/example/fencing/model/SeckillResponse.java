package com.example.fencing.model;

public record SeckillResponse(
        String productId,
        String userId,
        Long orderId,
        long fenceToken,
        long remainingStock,
        boolean replayed) { }

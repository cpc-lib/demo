package com.example.fencing.model;

public record OrderRecord(
        long orderId,
        String productId,
        String userId,
        long fenceToken) {
    public SeckillResponse toResponse(long remainingStock, boolean replayed) {
        return new SeckillResponse(productId, userId, orderId, fenceToken, remainingStock, replayed);
    }
}

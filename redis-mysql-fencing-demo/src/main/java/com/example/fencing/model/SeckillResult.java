package com.example.fencing.model;

/**
 * 秒杀执行结果：订单 + 商品扣减后的最新快照（事务内 SELECT，零额外 DB）。
 */
public record SeckillResult(OrderRecord order, ProductSnapshot productAfter) {
    public SeckillResponse toResponse(boolean replayed) {
        return new SeckillResponse(
                order.productId(), order.userId(), order.orderId(),
                order.fenceToken(), productAfter.stock(), replayed);
    }
}

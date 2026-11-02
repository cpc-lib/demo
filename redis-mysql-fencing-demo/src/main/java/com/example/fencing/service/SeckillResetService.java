package com.example.fencing.service;

import com.example.fencing.model.SeckillResetResponse;
import com.example.fencing.repository.ProductRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 重置秒杀数据（演示/压测后恢复现场）：
 *   1. 清空订单 + 幂等记录
 *   2. 库存恢复为 initial_stock，sold 归零（fence_token 保持单调，不回退）
 *   3. 事务提交后删除商品缓存，保证后续请求重新回源 DB
 */
@Service
public class SeckillResetService {
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final SeckillCacheService cache;

    public SeckillResetService(JdbcTemplate jdbc, ProductRepository products,
                               SeckillCacheService cache) {
        this.jdbc = jdbc;
        this.products = products;
        this.cache = cache;
    }

    @Transactional
    public SeckillResetResponse resetAll() {
        int operations = jdbc.update("DELETE FROM seckill_operation");
        int orders = jdbc.update("DELETE FROM seckill_order");
        int restored = jdbc.update("""
                UPDATE seckill_product SET stock = initial_stock, sold = 0
                """);

        List<String> productIds = products.findAll().stream()
                .map(p -> p.productId())
                .toList();

        // 必须在事务提交后清缓存，否则并发读可能把旧库存重新写回缓存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productIds.forEach(cache::evict);
            }
        });

        return new SeckillResetResponse(restored, orders, operations);
    }
}

package com.example.fencing.service;

import com.example.fencing.error.AlreadyPurchasedException;
import com.example.fencing.error.SeckillStockOutException;
import com.example.fencing.error.StaleFenceException;
import com.example.fencing.model.OrderRecord;
import com.example.fencing.model.ProductSnapshot;
import com.example.fencing.model.SeckillResult;
import com.example.fencing.repository.OrderRepository;
import com.example.fencing.repository.ProductRepository;
import com.example.fencing.repository.SeckillOperationRepository;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀核心业务：库存扣减（带 fence_token 条件）+ 一人一单。
 * 必须在已持有商品级 Redis 锁的前提下调用。
 *
 * 优化点：UPDATE 成功后事务内顺手 SELECT 商品最新状态，
 * 一路往上带给 Coordinator 用于刷缓存和构造 response，零额外 DB。
 */
@Service
public class SeckillService {
    private final JdbcTemplate jdbc;
    private final OrderRepository orders;
    private final SeckillOperationRepository operations;

    public SeckillService(JdbcTemplate jdbc, OrderRepository orders,
                          SeckillOperationRepository operations) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.operations = operations;
    }

    @Transactional
    public SeckillResult execute(String productId, String requestId,
                                 String userId, long token) {
        // 1) 事务内幂等重放
        Optional<OrderRecord> replayed = operations.find(productId, requestId);
        if (replayed.isPresent()) {
            return new SeckillResult(replayed.get(), loadProductSnapshot(productId));
        }

        // 2) 事务内再查一次一人一单
        if (orders.countByProductAndUser(productId, userId) > 0) {
            throw new AlreadyPurchasedException(productId, userId);
        }

        // 3) 带 fence_token 条件扣库存
        int changed = jdbc.update("""
                UPDATE seckill_product
                SET stock = stock - 1, sold = sold + 1
                WHERE product_id = ? AND fence_token = ? AND stock > 0
                """, productId, token);
        if (changed == 0) {
            // UPDATE 失败：事务内查 DB 区分"售罄 vs stale fence"
            var p = loadProductSnapshot(productId);
            if (p.stock() <= 0) {
                throw new SeckillStockOutException(productId);
            }
            throw new StaleFenceException(productId, token);
        }

        // 4) 事务内查一次商品最新状态 → 零额外 DB
        ProductSnapshot snap = loadProductSnapshot(productId);

        // 5) 创建订单（唯一索引兜底）
        OrderRecord order;
        try {
            order = orders.insert(productId, userId, token);
        } catch (DuplicateKeyException dke) {
            throw new AlreadyPurchasedException(productId, userId);
        }

        // 6) 写幂等记录
        operations.insert(productId, requestId, userId, token, order.orderId());

        return new SeckillResult(order, snap);
    }

    /** 事务内 SELECT 商品最新状态（已提交的 UPDATE 之后读）。 */
    private ProductSnapshot loadProductSnapshot(String productId) {
        return jdbc.query("""
                SELECT product_id, name, stock, sold, fence_token
                FROM seckill_product WHERE product_id = ?
                """, rs -> {
            if (!rs.next()) {
                throw new IllegalStateException("Product missing: " + productId);
            }
            return new ProductSnapshot(
                    rs.getString("product_id"),
                    rs.getString("name"),
                    rs.getLong("stock"),
                    rs.getLong("sold"),
                    rs.getLong("fence_token"));
        }, productId);
    }
}

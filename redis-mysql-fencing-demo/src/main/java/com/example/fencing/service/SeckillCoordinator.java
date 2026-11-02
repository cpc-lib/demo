package com.example.fencing.service;

import com.example.fencing.error.AlreadyPurchasedException;
import com.example.fencing.error.IdempotencyConflictException;
import com.example.fencing.error.LockBusyException;
import com.example.fencing.error.LockInterruptedException;
import com.example.fencing.error.LockLostException;
import com.example.fencing.error.ProductNotFoundException;
import com.example.fencing.error.RateLimitExceededException;
import com.example.fencing.error.SeckillStockOutException;
import com.example.fencing.model.OrderRecord;
import com.example.fencing.model.ProductSnapshot;
import com.example.fencing.model.SeckillResponse;
import com.example.fencing.model.SeckillResult;
import com.example.fencing.repository.OrderRepository;
import com.example.fencing.repository.ProductRepository;
import com.example.fencing.repository.SeckillOperationRepository;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 秒杀协调器 —— 完整请求流水线：
 *
 *   [穿透]  → 布隆过滤器挡不存在的 productId
 *   [击穿]  → 商品缓存（逻辑过期 + 互斥重建）
 *   [雪崩]  → 缓存 TTL 随机抖动
 *   [限流]  → 商品级 + 用户级两层令牌桶
 *   [锁]    → Redis 分布式锁（商品粒度）
 *   [业务]  → fence_token 发号 + DB 事务扣库存建订单
 *
 * 所有快速拒绝都在抢锁之前，避免把锁变成垃圾请求的垃圾桶。
 */
@Service
public class SeckillCoordinator {
    private static final Logger log = LoggerFactory.getLogger(SeckillCoordinator.class);

    private final RedissonClient redisson;
    private final SeckillBloomFilter bloom;
    private final SeckillCacheService cache;
    private final RateLimitService rl;
    private final SeckillFenceIssuer fenceIssuer;
    private final SeckillService seckill;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final SeckillOperationRepository operations;
    private final long waitSeconds;

    public SeckillCoordinator(RedissonClient redisson,
                              SeckillBloomFilter bloom,
                              SeckillCacheService cache,
                              RateLimitService rl,
                              SeckillFenceIssuer fenceIssuer,
                              SeckillService seckill,
                              ProductRepository products,
                              OrderRepository orders,
                              SeckillOperationRepository operations,
                              @Value("${app.lock.wait-seconds:2}") long waitSeconds) {
        this.redisson = redisson;
        this.bloom = bloom;
        this.cache = cache;
        this.rl = rl;
        this.fenceIssuer = fenceIssuer;
        this.seckill = seckill;
        this.products = products;
        this.orders = orders;
        this.operations = operations;
        this.waitSeconds = waitSeconds;
    }

    public SeckillResponse buy(String productId, String requestId, String userId) {
        // ─── [1] 穿透防护 ──────────────────────────────────────────
        // 布隆说"一定不存在" → 直接拒绝，零 Redis/DB 开销
        if (!bloom.mightContain(productId)) {
            throw new ProductNotFoundException(productId);
        }

        // ─── [2] 商品缓存（击穿 + 雪崩 + 穿透兜底）──────────────────
        // 击穿：逻辑过期 + 互斥重建
        // 雪崩：随机 TTL
        // 穿透兜底：空值缓存（DB 不存在时写 nullKey，60s）
        var snap = cache.get(productId);
        if (snap == null) {
            // 布隆误判 or 空值缓存命中 → 商品真的不存在
            throw new ProductNotFoundException(productId);
        }

        // 缓存里就没库存 → 快速拒绝（秒杀热点场景这条路径上的请求最多）
        if (snap.stock() <= 0) {
            throw new SeckillStockOutException(productId);
        }

        // ─── [3] 锁外快速幂等重放（减少锁竞争）───────────────────────
        Optional<OrderRecord> replay = operations.find(productId, requestId);
        if (replay.isPresent()) {
            OrderRecord rec = replay.get();
            if (!rec.userId().equals(userId)) {
                throw new IdempotencyConflictException(
                        "request_id belongs to user " + rec.userId() + " but caller is " + userId);
            }
            return rec.toResponse(remainingStock(productId), true);
        }

        // ─── [4] 两层限流（都在抢锁之前）─────────────────────────────
        // 商品整体 QPS 限：防止整款商品被打爆
        if (!rl.tryAcquireProduct(productId)) {
            throw new RateLimitExceededException("prod:" + productId);
        }
        // 用户对单商品 QPS 限：防刷
        if (!rl.tryAcquireUser(productId, userId)) {
            throw new RateLimitExceededException("user:" + productId + ":" + userId);
        }

        // ─── [5] 抢商品级分布式锁 ────────────────────────────────────
        RLock lock = redisson.getLock("lock:seckill:{" + productId + "}");
        boolean acquired;
        try {
            acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockInterruptedException(productId, e);
        }
        if (!acquired) {
            throw new LockBusyException(productId);
        }

        try {
            assertStillOwnLock(lock, productId);

            // ─── [6] 锁内前置校验 ──────────────────────────────────
            // 6a) 再次查 DB 确认（缓存可能 stale；锁内校验才是权威）
            var product = products.find(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            if (product.stock() <= 0) {
                throw new SeckillStockOutException(productId);
            }

            // 6b) 一人一单前置（锁内串行化，count>0 即已买过）
            if (orders.countByProductAndUser(productId, userId) > 0) {
                throw new AlreadyPurchasedException(productId, userId);
            }

            assertStillOwnLock(lock, productId);

            // ─── [7] MySQL 独立事务发 fence_token ──────────────────
            long token = fenceIssuer.issue(productId);

            assertStillOwnLock(lock, productId);

            // ─── [8] 事务内原子扣库存 + 建订单 + 拿最新商品快照 ────
            SeckillResult result = seckill.execute(productId, requestId, userId, token);

            // ─── [9] 秒杀成功 → 事务已提交 → 零额外 DB 刷缓存 + 构造 response ────
            cache.refresh(result.productAfter());
            return result.toResponse(false);

        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                } else {
                    log.warn("Lost Redis lock before unlock for product {}", productId);
                }
            } catch (Exception unlockFailure) {
                log.error("Redis lock release failed for product {}", productId, unlockFailure);
            }
        }
    }

    private void assertStillOwnLock(RLock lock, String resourceId) {
        if (!lock.isHeldByCurrentThread()) {
            throw new LockLostException(resourceId);
        }
    }

    private long remainingStock(String productId) {
        var snap = cache.get(productId);
        if (snap != null) return snap.stock();
        return products.find(productId).map(p -> p.stock()).orElse(0L);
    }
}

package com.example.fencing.service;

import com.example.fencing.model.ProductSnapshot;
import com.example.fencing.repository.ProductRepository;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 秒杀商品 Redis 缓存 —— 解决三大问题：
 *   雪崩 ：TTL = baseSeconds + random(0..jitterSeconds)
 *   击穿 ：逻辑过期 + 互斥锁重建；其他线程返回旧值（安全但可能 stale）
 *   穿透 ：空值标记 seckill:product:null:{productId} + 短 TTL
 *
 * 缓存只做前置快速拒绝，权威正确性仍在 MySQL（fence_token + 唯一索引）。
 */
@Service
public class SeckillCacheService {
    private static final Logger log = LoggerFactory.getLogger(SeckillCacheService.class);

    private final RedissonClient redisson;
    private final ProductRepository products;
    private final long ttlBaseSeconds;
    private final long ttlJitterSeconds;
    private final long logicalExpireBeforeSeconds;
    private final long nullTtlSeconds;

    private final Random rnd = new Random();

    public SeckillCacheService(RedissonClient redisson, ProductRepository products,
                               @Value("${app.seckill.cache.ttl-seconds}") long ttlBaseSeconds,
                               @Value("${app.seckill.cache.ttl-jitter-seconds}") long ttlJitterSeconds,
                               @Value("${app.seckill.cache.logical-expire-before-seconds}") long logicalExpireBeforeSeconds,
                               @Value("${app.seckill.cache.null-ttl-seconds}") long nullTtlSeconds) {
        this.redisson = redisson;
        this.products = products;
        this.ttlBaseSeconds = ttlBaseSeconds;
        this.ttlJitterSeconds = ttlJitterSeconds;
        this.logicalExpireBeforeSeconds = logicalExpireBeforeSeconds;
        this.nullTtlSeconds = nullTtlSeconds;
    }

    // ---- Key helpers ----
    private String cacheKey(String productId) {
        return "seckill:product:{" + productId + "}";
    }
    private String nullKey(String productId) {
        return "seckill:product:null:{" + productId + "}";
    }
    private String rebuildLockKey(String productId) {
        return "seckill:rebuild:{" + productId + "}";
    }

    /**
     * 获取商品缓存快照。
     * 可能返回 stale（击穿窗口内旧值），但永远不会返回 null 表示"商品不存在"——
     * 不存在会抛 IllegalStateException，由上层决定怎么处理。
     */
    public ProductSnapshot get(String productId) {
        // 1) 穿透防护：空值标记存在 → 商品不存在
        var nullBucket = redisson.getBucket(nullKey(productId));
        if (Boolean.TRUE.equals(nullBucket.isExists())) {
            return null;
        }

        // 2) 读主缓存 Hash
        RMap<String, String> cache = redisson.getMap(cacheKey(productId));
        Map<String, String> entry = cache.readAllMap();
        if (entry.isEmpty()) {
            return loadAndCache(productId);
        }

        // 3) 命中 → 检查逻辑过期
        long logicalExpire = Long.parseLong(entry.getOrDefault("logicalExpireAt", "0"));
        long now = System.currentTimeMillis();
        if (now < logicalExpire) {
            return toSnapshot(entry);
        }

        // 4) 逻辑过期 → 抢互斥锁重建
        RLock rebuildLock = redisson.getLock(rebuildLockKey(productId));
        boolean gotLock;
        try {
            gotLock = rebuildLock.tryLock(0, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            gotLock = false;
        }

        if (gotLock) {
            try {
                // Double-check：抢到锁后再读一次，可能别人刚重建完
                Map<String, String> recheck = cache.readAllMap();
                long recheckExpire = Long.parseLong(recheck.getOrDefault("logicalExpireAt", "0"));
                if (System.currentTimeMillis() < recheckExpire) {
                    return toSnapshot(recheck);
                }
                return loadAndCache(productId);
            } finally {
                if (rebuildLock.isHeldByCurrentThread()) {
                    rebuildLock.unlock();
                }
            }
        }

        // 5) 没抢到锁 → 返回旧值（击穿窗口内，安全但可能 stale）
        log.debug("Cache stale for product {} (rebuild in progress), returning old snapshot", productId);
        return toSnapshot(entry);
    }

    /**
     * 秒杀成功后同步刷缓存 —— 零额外 DB。
     * 调用方传入的 ProductSnapshot 是在 DB 事务内刚查出来的最新值。
     */
    public void refresh(ProductSnapshot snapshot) {
        writeCache(snapshot.productId(), snapshot);
    }

    /** 删除商品相关缓存（重置数据时用）。 */
    public void evict(String productId) {
        redisson.getMap(cacheKey(productId)).delete();
        redisson.getBucket(nullKey(productId)).delete();
        redisson.getKeys().delete(rebuildLockKey(productId));
    }

    // ---- 内部：回源 DB + 写缓存 ----

    private ProductSnapshot loadAndCache(String productId) {
        var snapshot = products.find(productId);
        if (snapshot.isEmpty()) {
            // 穿透防护：写空值标记，短 TTL
            redisson.getBucket(nullKey(productId)).set("1", nullTtlSeconds, TimeUnit.SECONDS);
            return null;
        }
        writeCache(productId, snapshot.get());
        return snapshot.get();
    }

    private void writeCache(String productId, ProductSnapshot snap) {
        // 随机 TTL 防雪崩
        long ttl = ttlBaseSeconds + rnd.nextInt((int) ttlJitterSeconds + 1);
        long logicalExpireAt = System.currentTimeMillis()
                + (ttl - logicalExpireBeforeSeconds) * 1000L;

        RMap<String, String> cache = redisson.getMap(cacheKey(productId));
        cache.putAll(Map.of(
                "productId", snap.productId(),
                "name", snap.name(),
                "stock", String.valueOf(snap.stock()),
                "sold", String.valueOf(snap.sold()),
                "fenceToken", String.valueOf(snap.fenceToken()),
                "logicalExpireAt", String.valueOf(logicalExpireAt)));
        cache.expire(ttl, TimeUnit.SECONDS);
        log.debug("Cached product {} (stock={}, TTL={}s, logicalExpireAt={})",
                productId, snap.stock(), ttl, logicalExpireAt);
    }

    private ProductSnapshot toSnapshot(Map<String, String> entry) {
        return new ProductSnapshot(
                entry.get("productId"),
                entry.get("name"),
                Long.parseLong(entry.get("stock")),
                Long.parseLong(entry.get("sold")),
                Long.parseLong(entry.get("fenceToken")));
    }
}

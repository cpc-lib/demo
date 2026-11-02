package com.example.fencing.service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 秒杀布隆过滤器：缓存层穿透防护。
 * 启动时从 DB 加载所有 productId；不存在的 ID 一定被拒绝。
 *
 * Redisson RBloomFilter 是分片式的，cluster 模式下也能用。
 */
@Service
public class SeckillBloomFilter {
    private static final Logger log = LoggerFactory.getLogger(SeckillBloomFilter.class);

    private final RedissonClient redisson;
    private final JdbcTemplate jdbc;
    private final String filterName;
    private final long expectedCount;
    private final double falsePositiveRate;

    private RBloomFilter<String> bloom;

    public SeckillBloomFilter(RedissonClient redisson, JdbcTemplate jdbc,
                              @Value("${app.seckill.bloom.filter-name}") String filterName,
                              @Value("${app.seckill.bloom.expected-count}") long expectedCount,
                              @Value("${app.seckill.bloom.false-positive-rate}") double falsePositiveRate) {
        this.redisson = redisson;
        this.jdbc = jdbc;
        this.filterName = filterName;
        this.expectedCount = expectedCount;
        this.falsePositiveRate = falsePositiveRate;
    }

    @PostConstruct
    public synchronized void init() {
        this.bloom = redisson.getBloomFilter(filterName);
        // tryInit: 已初始化过就跳过，不会重复分配数组。
        if (bloom.tryInit(expectedCount, falsePositiveRate)) {
            log.info("Created new bloom filter '{}' (expected={}, fpr={})",
                    filterName, expectedCount, falsePositiveRate);
        } else {
            log.info("Reusing existing bloom filter '{}'", filterName);
        }
        reload();
    }

    /** 应用完全启动（Flyway 迁移已执行完）后再加载一次，确保 V3 新增商品不掉队。 */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reload();
    }

    /** 启动后（Flyway 已建表）从 DB 加载所有现有商品 ID。 */
    public void reload() {
        try {
            List<String> ids = jdbc.queryForList(
                    "SELECT product_id FROM seckill_product", String.class);
            for (String id : ids) {
                bloom.add(id);
            }
            log.info("Bloom filter loaded {} product IDs", ids.size());
        } catch (Exception e) {
            // Flyway 还没跑 seckill 表 → 启动早期可能不存在，忽略等后续 reload。
            log.warn("Bloom filter reload deferred (table may not exist yet): {}", e.getMessage());
        }
    }

    /** 商品是否可能存在：false 一定不存在；true 可能存在（有极低误判率）。 */
    public boolean mightContain(String productId) {
        return bloom.contains(productId);
    }

    /** 新增商品时同步塞进去。 */
    public void add(String productId) {
        bloom.add(productId);
    }
}

package com.example.distributedlock.service;

import com.example.distributedlock.lock.DistributedLockExecutor;
import com.example.distributedlock.lock.LockType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;
    private final DistributedLockExecutor lockExecutor;
    private final long criticalSectionDelayMs;

    public InventoryService(
            JdbcTemplate jdbcTemplate,
            DistributedLockExecutor lockExecutor,
            @Value("${app.demo.critical-section-delay-ms:80}") long criticalSectionDelayMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.lockExecutor = lockExecutor;
        this.criticalSectionDelayMs = criticalSectionDelayMs;
    }

    public void reset(long productId, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must be >= 0");
        }
        jdbcTemplate.update("""
                INSERT INTO demo_inventory(product_id, stock, version)
                VALUES (?, ?, 0)
                ON DUPLICATE KEY UPDATE stock = VALUES(stock), version = 0
                """, productId, stock);
    }

    public int getStock(long productId) {
        List<Integer> stocks = jdbcTemplate.queryForList(
                "SELECT stock FROM demo_inventory WHERE product_id = ?",
                Integer.class,
                productId
        );
        if (stocks.isEmpty()) {
            throw new IllegalArgumentException("product not found: " + productId);
        }
        return stocks.get(0);
    }

    public boolean decrementWithLock(LockType type, long productId) {
        String key = "inventory:" + productId;
        return lockExecutor.execute(
                type,
                key,
                Duration.ofSeconds(3),
                Duration.ofSeconds(10),
                () -> decrementUnsafe(productId)
        );
    }

    /**
     * Deliberately uses read -> delay -> write rather than a single atomic SQL update,
     * so concurrent calls demonstrate the lost-update problem without a distributed lock.
     */
    public boolean decrementUnsafe(long productId) {
        int current = getStock(productId);
        if (current <= 0) {
            return false;
        }

        simulateBusinessWork();
        jdbcTemplate.update(
                "UPDATE demo_inventory SET stock = ?, version = version + 1 WHERE product_id = ?",
                current - 1,
                productId
        );
        return true;
    }

    private void simulateBusinessWork() {
        try {
            Thread.sleep(criticalSectionDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }
}

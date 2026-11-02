package com.example.fencing.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 seckill_product 表上发单调递增的 fence_token。
 * 每次秒杀请求都先独立事务发号，业务写带条件防 stale token。
 */
@Service
public class SeckillFenceIssuer {
    private final JdbcTemplate jdbc;

    public SeckillFenceIssuer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long issue(String productId) {
        // 商品一定已存在（启动时 INSERT 了 SKU-001），纯 UPDATE 即可。
        int affected = jdbc.update("""
                UPDATE seckill_product
                SET fence_token = fence_token + 1
                WHERE product_id = ? AND fence_token < 9223372036854775807
                """, productId);
        if (affected != 1) {
            throw new IllegalStateException("Seckill fence token exhausted or product missing: " + productId);
        }
        Long token = jdbc.queryForObject("""
                SELECT fence_token FROM seckill_product WHERE product_id = ?
                """, Long.class, productId);
        return token;
    }
}

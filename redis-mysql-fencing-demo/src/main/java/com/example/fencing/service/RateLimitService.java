package com.example.fencing.service;

import java.time.Duration;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 秒杀限流 —— 两层独立令牌桶：
 *   商品级：整款商品的总请求上限
 *   用户级：单用户对单商品的请求上限
 *
 * 用 Redisson RRateLimiter（线程安全令牌桶），比自写 INCR+TTL 更稳。
 * 两层限流都放在抢分布式锁之前，避免把锁变成无效请求的垃圾桶。
 */
@Service
public class RateLimitService {
    private final RedissonClient redisson;
    private final long productQps;
    private final long userQps;

    public RateLimitService(RedissonClient redisson,
                            @Value("${app.seckill.ratelimit.product-qps}") long productQps,
                            @Value("${app.seckill.ratelimit.user-qps}") long userQps) {
        this.redisson = redisson;
        this.productQps = productQps;
        this.userQps = userQps;
    }

    /** 商品整体 QPS 限。 */
    public boolean tryAcquireProduct(String productId) {
        RRateLimiter limiter = redisson.getRateLimiter("seckill:rl:prod:{" + productId + "}");
        // trySetRate 幂等：已存在的桶不会被重置，返回 false。
        limiter.trySetRate(RateType.OVERALL, productQps, Duration.ofSeconds(1));
        return limiter.tryAcquire();
    }

    /** 用户对单个商品的 QPS 限。 */
    public boolean tryAcquireUser(String productId, String userId) {
        RRateLimiter limiter = redisson.getRateLimiter(
                "seckill:rl:user:{" + productId + "}:" + userId);
        limiter.trySetRate(RateType.OVERALL, userQps, Duration.ofSeconds(1));
        return limiter.tryAcquire();
    }
}

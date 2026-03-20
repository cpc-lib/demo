package com.example.limit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 核心 AOP 切面：
 *
 * 功能：
 *  1）先通过令牌桶（RRateLimiter）做“整体 QPS 限制”
 *  2）再通过分布式信号量（RSemaphore）做“最大并发 + 可选排队”
 *
 * 整体流程：
 *  1. 根据注解中的 key 拼接：
 *     - limit:sem:{key}
 *     - limit:ratelimiter:{key}
 *  2. 配置令牌桶速率（全局共享）
 *  3. 申请 1 个令牌：
 *     - 如果失败：说明 QPS 已达上限 => 直接抛异常
 *  4. 尝试申请并发许可：
 *     - enableQueue = true ：在 queueTimeoutMs 内阻塞等待
 *     - enableQueue = false：立即拿，如果失败就抛异常
 *  5. 执行目标方法
 *  6. finally 中释放并发许可
 */
@Aspect
@Component
public class QueueTokenLimitAspect {

    private final RedissonClient redissonClient;

    public QueueTokenLimitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint pjp, QueueTokenLimit limit) throws Throwable {

        // ====================== 1. 构建 Redis Key ======================
        String key = limit.key();
        String semKey = "limit:sem:" + key;           // 并发信号量的 key
        String rateLimiterKey = "limit:ratelimiter:" + key; // 令牌桶的 key

        // ====================== 2. 获取/初始化 令牌桶 ======================
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);

        // trySetRate 只在第一次会返回 true，后续调用会返回 false（即已配置过）
        // 这里每次都调用一次无妨，用于确保在“空 Redis / 清库之后”自动恢复配置。
        rateLimiter.trySetRate(
                RateType.OVERALL,                 // OVERALL：集群所有实例共享一个总体速率
                limit.tokenRate(),                // 每秒产生多少令牌（QPS 上限）
                1,                                // 时间间隔长度
                RateIntervalUnit.SECONDS          // 时间间隔单位：秒
        );

        // 2.1 尝试申请 1 个令牌
        boolean tokenAcquired = rateLimiter.tryAcquire();
        if (!tokenAcquired) {
            // 令牌桶拒绝，相当于整体 QPS 已经达到上限
            throw new LimitException("自适应令牌桶拒绝请求：" + limit.message());
        }

        // ====================== 3. 获取/初始化 并发信号量 ======================
        RSemaphore semaphore = redissonClient.getSemaphore(semKey);
        // 设置最大并发数（只在第一次生效）
        semaphore.trySetPermits(limit.maxConcurrency());

        boolean permitAcquired;

        // ====================== 4. 申请并发许可（支持排队等待） ======================
        if (limit.enableQueue()) {
            // enableQueue = true：在 queueTimeoutMs 内，阻塞等待并发许可
            permitAcquired = semaphore.tryAcquire(limit.queueTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!permitAcquired) {
                // 排队超时，直接抛出异常
                throw new LimitException("排队等待超时：" + limit.message());
            }
        } else {
            // enableQueue = false：非阻塞模式，立即尝试获取
            permitAcquired = semaphore.tryAcquire();
            if (!permitAcquired) {
                // 并发已满
                throw new LimitException("并发已满：" + limit.message());
            }
        }

        try {
            // ====================== 5. 执行目标方法 ======================
            return pjp.proceed();
        } finally {
            // ====================== 6. 释放并发许可 ======================
            // 一定要放在 finally 中，避免业务代码异常导致死锁
            if (permitAcquired) {
                semaphore.release();
            }
        }
    }
}

package com.example.limit;

import java.lang.annotation.*;

/**
 * 组合版限流注解：
 *  - 支持“令牌桶 + 并发数 + 可选排队等待”
 *  - 可以加在 Controller / Service / RPC 方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueueTokenLimit {

    /**
     * 最大并发数（分布式信号量）
     * 比如设置为 5，则同一时间最多只有 5 个请求在执行目标方法。
     */
    int maxConcurrency() default 5;

    /**
     * 限流 key（必填），多实例共享。
     * 相同 key 会共用同一组：
     *  - RSemaphore（并发控制）
     *  - RRateLimiter（令牌桶）
     *
     * 通常可以设计成：业务名:接口名 / 租户:接口名 / IP:接口名 等
     */
    String key();

    /**
     * 是否允许排队等待：
     *  - true：拿不到并发许可时，允许在一定时间内排队等待
     *  - false：拿不到并发许可直接抛出异常
     */
    boolean enableQueue() default true;

    /**
     * 排队最长等待时间（毫秒），仅在 enableQueue = true 时生效。
     * 一般建议控制在 1~3 秒内，避免排队过长导致大面积超时。
     */
    long queueTimeoutMs() default 3000;

    /**
     * 令牌生成速率：
     *  - 单位：每秒多少个令牌（QPS 限制）
     *  - 比如设置为 20，则每秒全局最多允许 20 个请求通过。
     */
    int tokenRate() default 20;

    /**
     * 令牌桶“初始容量 / 最大瞬时突发”建议值。
     * 说明：
     *  - Redisson 的 RRateLimiter 内部并没有“桶容量”参数，
     *    但我们可以用 tokenRate × 一个时间窗口 来估算合理的突发能力。
     *  - 这里先留作语义字段，可用于动态限流（例如把配置放入 Nacos 后通过它控制）。
     */
    int tokenBucketSize() default 40;

    /**
     * 默认错误提示文案。
     */
    String message() default "当前服务繁忙，请稍后重试";
}

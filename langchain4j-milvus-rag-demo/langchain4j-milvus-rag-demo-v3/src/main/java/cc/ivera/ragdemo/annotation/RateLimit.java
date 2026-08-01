package cc.ivera.ragdemo.annotation;

import java.lang.annotation.*;

/**
 * 限流注解 - 基于 Redis ZSET 实现滑动窗口限流
 * 
 * 使用方式：
 * @RateLimit(max = 100, windowSeconds = 60, key = RateLimit.KeyType.IP)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 最大请求数
     */
    int max() default 100;

    /**
     * 窗口大小（秒）
     */
    int windowSeconds() default 60;

    /**
     * 限流维度
     */
    KeyType key() default KeyType.IP;

    /**
     * 限流维度类型
     */
    enum KeyType {
        /**
         * 按 IP 限流
         */
        IP,
        /**
         * 按用户 ID 限流
         */
        USER,
        /**
         * 按租户 ID 限流
         */
        TENANT,
        /**
         * 按 IP + URI 限流
         */
        IP_URI,
        /**
         * 按租户 + URI 限流
         */
        TENANT_URI
    }
}

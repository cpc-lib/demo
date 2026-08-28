package cc.ivera.ragdemo.service.ratelimit;

import cc.ivera.ragdemo.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 滑动窗口限流服务
 * 使用 Redis ZSET + Lua 脚本实现精确的滑动窗口限流
 * 
 * 核心原理：
 * 1. 将每次请求的时间戳作为 score 和 member 存入 ZSET
 * 2. Lua 脚本原子执行：ZREMRANGEBYSCORE → ZCARD → ZADD → EXPIRE
 * 3. 超过阈值时抛出 RateLimitException
 */
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class SlidingWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua 脚本：滑动窗口限流
     * KEYS[1]: 限流 key
     * ARGV[1]: 窗口大小（毫秒）
     * ARGV[2]: 最大请求数
     * ARGV[3]: 当前时间戳（毫秒）
     * 
     * 返回值：
     * -1: 超过限流阈值
     * >=0: 当前窗口内请求数（包括本次请求）
     */
    private static final String SLIDING_WINDOW_LUA = """
            local key = KEYS[1]
            local windowMs = tonumber(ARGV[1])
            local maxCount = tonumber(ARGV[2])
            local currentTime = tonumber(ARGV[3])
            
            -- 计算窗口起始时间（当前时间 - 窗口大小）
            local windowStart = currentTime - windowMs
            
            -- 1. 移除窗口外的旧记录（滑动窗口核心操作）
            redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
            
            -- 2. 统计当前窗口内的请求数
            local currentCount = redis.call('ZCARD', key)
            
            -- 3. 判断是否超过阈值
            if currentCount >= maxCount then
                -- 超过阈值，返回 -1
                return -1
            end
            
            -- 4. 将当前请求时间戳加入 ZSET（score 和 member 都是时间戳+随机数，避免重复）
            redis.call('ZADD', key, currentTime, tostring(currentTime) .. '_' .. redis.call('RANDOMKEY'))
            
            -- 5. 设置过期时间（避免内存泄漏，过期时间比窗口大一点）
            redis.call('EXPIRE', key, windowMs / 1000 + 1)
            
            -- 6. 返回当前窗口内请求数（包括本次）
            return currentCount + 1
            """;

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(SLIDING_WINDOW_LUA, Long.class);

    /**
     * 尝试获取限流许可
     * 
     * @param key 限流 key
     * @param maxCount 最大请求数
     * @param windowSeconds 窗口大小（秒）
     * @return 当前窗口内请求数（包括本次请求）
     * @throws RateLimitException 超过限流阈值时抛出
     */
    public long tryAcquire(String key, int maxCount, int windowSeconds) {
        try {
            long currentTime = Instant.now().toEpochMilli();
            long windowMs = (long) windowSeconds * 1000;
            
            List<String> keys = Collections.singletonList(key);
            
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    keys,
                    String.valueOf(windowMs),
                    String.valueOf(maxCount),
                    String.valueOf(currentTime)
            );
            
            if (result == null || result < 0) {
                log.warn("Rate limit exceeded: key={}, max={}, window={}s", key, maxCount, windowSeconds);
                throw new RateLimitException(
                        "Too many requests",
                        windowSeconds,
                        key
                );
            }
            
            return result;
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时采用 fail-open 策略（放行请求但记录告警）
            log.error("Rate limiter Redis error: key={}, error={}", key, e.getMessage());
            return 0; // 0 表示未限流（fail-open）
        }
    }

    /**
     * 获取当前窗口内的请求数（用于监控）
     * 
     * @param key 限流 key
     * @param windowSeconds 窗口大小（秒）
     * @return 当前窗口内请求数
     */
    public long getCurrentCount(String key, int windowSeconds) {
        try {
            long currentTime = Instant.now().toEpochMilli();
            long windowStart = currentTime - (long) windowSeconds * 1000;
            
            // 先清理窗口外的旧记录
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            
            // 统计当前窗口内的请求数
            Long count = redisTemplate.opsForZSet().size(key);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to get rate limit count: key={}, error={}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * 重置限流计数器
     * 
     * @param key 限流 key
     */
    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to reset rate limit: key={}, error={}", key, e.getMessage());
        }
    }
}

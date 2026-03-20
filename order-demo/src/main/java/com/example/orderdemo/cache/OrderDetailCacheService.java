package com.example.orderdemo.cache;

import com.example.orderdemo.config.OrderCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class OrderDetailCacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final OrderCacheProperties props;

  /**
   * 读取订单详情（版本化缓存）：
   * 1) 读 version
   * 2) 用 version 拼 detailKey 读详情
   * 3) miss -> 加锁回源 DB -> 写入 detailKey
   */
  @SuppressWarnings("unchecked")
  public <T> T getOrLoad(Long orderId, Class<T> type, Supplier<T> dbLoader) {
    long ver = getOrInitVersion(orderId);

    String dKey = OrderCacheKeys.detailKey(orderId, ver);
    Object cached = redisTemplate.opsForValue().get(dKey);
    if (cached != null) {
      return cast(cached, type);
    }

    // 缓存击穿保护：短锁（setIfAbsent）
    String lKey = OrderCacheKeys.lockKey(orderId);
    boolean locked = Boolean.TRUE.equals(
        redisTemplate.opsForValue().setIfAbsent(lKey, "1", Duration.ofMillis(props.getLockTtlMs()))
    );

    if (locked) {
      try {
        // double check
        cached = redisTemplate.opsForValue().get(dKey);
        if (cached != null) {
          return cast(cached, type);
        }

        T db = dbLoader.get();
        if (db != null) {
          // 详情 TTL 可以加一点随机抖动，降低同一时刻集中失效
          long jitter = ThreadLocalRandom.current().nextLong(0, 30);
          redisTemplate.opsForValue().set(dKey, db, Duration.ofSeconds(props.getDetailTtlSeconds() + jitter));
        }
        return db;
      } finally {
        redisTemplate.delete(lKey);
      }
    } else {
      // 没抢到锁：稍微等一下再读（避免大家都打 DB）
      try {
        Thread.sleep(50);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      cached = redisTemplate.opsForValue().get(dKey);
      if (cached != null) {
        return cast(cached, type);
      }
      // 兜底：仍然 miss，就直接回源一次（极端情况下）
      return dbLoader.get();
    }
  }

  /** 版本号自增：写路径只需要 bump version，就能让旧缓存自然失效 */
  public long bumpVersion(Long orderId) {
    String vKey = OrderCacheKeys.versionKey(orderId);
    Long ver = redisTemplate.opsForValue().increment(vKey);
    // 确保版本 key TTL 足够长
    redisTemplate.expire(vKey, Duration.ofSeconds(props.getVersionTtlSeconds()));
    return ver == null ? 1L : ver;
  }

  /** 获取版本号，不存在则初始化为 1 */
  public long getOrInitVersion(Long orderId) {
    String vKey = OrderCacheKeys.versionKey(orderId);
    Object v = redisTemplate.opsForValue().get(vKey);
    if (v instanceof Number) {
      return ((Number) v).longValue();
    }
    // 初始化
    Boolean ok = redisTemplate.opsForValue().setIfAbsent(vKey, 1L, Duration.ofSeconds(props.getVersionTtlSeconds()));
    if (Boolean.TRUE.equals(ok)) {
      return 1L;
    }
    // 并发下再读一次
    v = redisTemplate.opsForValue().get(vKey);
    if (v instanceof Number) {
      return ((Number) v).longValue();
    }
    return 1L;
  }

  private <T> T cast(Object obj, Class<T> type) {
    if (type.isInstance(obj)) return type.cast(obj);
    // Jackson2JsonRedisSerializer<Object> 反序列化后通常是 LinkedHashMap，需要你用 DTO/VO 存储更稳
    // 本 demo 直接存 OrderDetailVO 对象，正常会是实例
    throw new IllegalStateException("Redis cached type mismatch: " + obj.getClass());
  }
}

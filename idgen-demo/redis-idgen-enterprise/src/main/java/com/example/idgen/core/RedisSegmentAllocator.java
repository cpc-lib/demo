package com.example.idgen.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Allocate sequence segments from Redis counter using atomic INCRBY.
 *
 * Key format is decided by caller (already includes tenant/biz/timeBucket).
 */
@Component
public class RedisSegmentAllocator {
    private static final Logger log = LoggerFactory.getLogger(RedisSegmentAllocator.class);

    private final StringRedisTemplate redis;

    public RedisSegmentAllocator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Segment allocate(String counterKey, int step) {
        Long newVal = redis.opsForValue().increment(counterKey, step);
        if (newVal == null) {
            throw new IllegalStateException("Redis INCRBY returned null for key=" + counterKey);
        }
        long end = newVal;
        long start = end - step + 1;
        return new Segment(start, end);
    }
}

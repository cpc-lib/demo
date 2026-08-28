package com.example.idgen.core;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSegmentAllocator {

    private final StringRedisTemplate redisTemplate;

    public RedisSegmentAllocator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Segment allocate(String bizKey, int step) {
        String key = "idgen:" + bizKey + ":counter";
        Long newVal = redisTemplate.opsForValue().increment(key, step);
        if (newVal == null) {
            throw new IllegalStateException("Redis INCRBY failed");
        }
        long end = newVal;
        long start = end - step + 1;
        return new Segment(start, end);
    }
}

package com.example.scanlogin.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;

/** One-time captcha answers: written with a TTL, consumed exactly once via GETDEL. */
@Repository
public class CaptchaStore {
    private static final String PREFIX = "auth:captcha:";
    private static final DefaultRedisScript<String> GET_DEL = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]) if value then redis.call('DEL', KEYS[1]) end return value",
            String.class);
    private final StringRedisTemplate redis;

    public CaptchaStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String id, String normalizedCode, Duration ttl) {
        redis.opsForValue().set(PREFIX + id, normalizedCode, ttl);
    }

    /** Returns the stored answer and immediately invalidates it, so each captcha can be tried once. */
    public String consume(String id) {
        return redis.execute(GET_DEL, Collections.singletonList(PREFIX + id));
    }

    public String peek(String id) {
        return redis.opsForValue().get(PREFIX + id);
    }
}

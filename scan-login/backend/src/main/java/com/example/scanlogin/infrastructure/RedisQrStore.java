package com.example.scanlogin.infrastructure;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.Collections;

@Repository
public class RedisQrStore {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<String> create = script("create.lua");
    private final DefaultRedisScript<String> scan = script("scan.lua");
    private final DefaultRedisScript<String> confirm = script("confirm.lua");
    private final DefaultRedisScript<String> consume = script("consume.lua");

    public RedisQrStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static DefaultRedisScript<String> script(String name) {
        DefaultRedisScript<String> result = new DefaultRedisScript<>();
        result.setLocation(new ClassPathResource("lua/" + name));
        result.setResultType(String.class);
        return result;
    }

    public static String key(String ticket) {
        return "auth:qr:login:" + ticket;
    }

    private String exec(DefaultRedisScript<String> script, String ticket, String... args) {
        return redis.execute(script, Collections.singletonList(key(ticket)), (Object[]) args);
    }

    public void create(String ticket, String hash, String code, int ttlSeconds) {
        String result = exec(create, ticket, hash, code, Integer.toString(ttlSeconds));
        if (!"CREATED".equals(result)) throw new IllegalStateException("Redis QR initialization failed");
    }

    public String scan(String ticket, long userId) {
        return exec(scan, ticket, Long.toString(userId));
    }

    public String confirm(String ticket, long userId) {
        return exec(confirm, ticket, Long.toString(userId));
    }

    public String consume(String ticket, String hash, long userId) {
        return exec(consume, ticket, hash, Long.toString(userId));
    }

    /** One HGETALL avoids mixing state and browser data from different reads. */
    public Snapshot snapshot(String ticket) {
        var values = redis.opsForHash().entries(key(ticket));
        if (values.isEmpty()) return null;
        return new Snapshot(asString(values.get("status")), asString(values.get("binding")),
                asString(values.get("code")), asString(values.get("userId")));
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public String field(String ticket, String field) {
        return asString(redis.opsForHash().get(key(ticket), field));
    }

    public record Snapshot(String status, String bindingHash, String code, String userId) {}
}

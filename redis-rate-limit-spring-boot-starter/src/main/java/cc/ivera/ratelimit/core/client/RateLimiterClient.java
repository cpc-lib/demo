package cc.ivera.ratelimit.core.client;

import cc.ivera.ratelimit.core.exception.RateLimitException;
import cc.ivera.ratelimit.core.rule.RateLimitRule;
import cc.ivera.ratelimit.core.executor.RedisRateLimitExecutor;

public class RateLimiterClient {

    private final RedisRateLimitExecutor executor;

    public RateLimiterClient(RedisRateLimitExecutor executor) {
        this.executor = executor;
    }

    public RateLimitResult tryAcquire(String key, RateLimitRule rule) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("key is blank");
        if (rule == null) throw new IllegalArgumentException("rule is null");
        return executor.acquire(key, rule);
    }

    public void acquireOrThrow(String key, RateLimitRule rule) {
        RateLimitResult r = tryAcquire(key, rule);
        if (!r.isAllowed()) {
            throw new RateLimitException("Rate limited. resetMillis=" + r.getResetMillis());
        }
    }
}

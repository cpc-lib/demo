package cc.ivera.ratelimit.core.executor;

import cc.ivera.ratelimit.core.config.RateLimitProperties;
import cc.ivera.ratelimit.core.client.RateLimitResult;
import cc.ivera.ratelimit.core.exception.RateLimitException;
import cc.ivera.ratelimit.core.policy.FailPolicy;
import cc.ivera.ratelimit.core.rule.FixedWindowRule;
import cc.ivera.ratelimit.core.rule.RateLimitRule;
import cc.ivera.ratelimit.core.rule.SlidingWindowRule;
import cc.ivera.ratelimit.core.rule.TokenBucketRule;
import cc.ivera.ratelimit.core.client.RedisScriptRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

public class RedisRateLimitExecutor {

    private final StringRedisTemplate redis;
    private final RedisScriptRegistry registry;
    private final RateLimitProperties props;

    public RedisRateLimitExecutor(StringRedisTemplate redis, RedisScriptRegistry registry, RateLimitProperties props) {
        this.redis = redis;
        this.registry = registry;
        this.props = props;
    }

    public RateLimitResult acquire(String rawKey, RateLimitRule rule) {
        String key = props.getKeyPrefix() + rawKey;
        long now = System.currentTimeMillis();

        try {
            List<String> keys = Collections.singletonList(key);
            List<?> ret;

            switch (rule.strategy()) {
                case FIXED_WINDOW: {
                    FixedWindowRule r = (FixedWindowRule) rule;
                    ret = redis.execute(registry.script(rule.strategy()), keys,
                            String.valueOf(r.getLimit()),
                            String.valueOf(r.getWindowMillis()));
                    break;
                }
                case SLIDING_WINDOW: {
                    SlidingWindowRule r = (SlidingWindowRule) rule;
                    ret = redis.execute(registry.script(rule.strategy()), keys,
                            String.valueOf(r.getLimit()),
                            String.valueOf(r.getWindowMillis()),
                            String.valueOf(now));
                    break;
                }
                case TOKEN_BUCKET: {
                    TokenBucketRule r = (TokenBucketRule) rule;
                    ret = redis.execute(registry.script(rule.strategy()), keys,
                            String.valueOf(r.getCapacity()),
                            String.valueOf(r.getRefillPerSecond()),
                            String.valueOf(now),
                            String.valueOf(r.permits()));
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown strategy: " + rule.strategy());
            }

            if (ret == null || ret.size() < 3) {
                throw new IllegalStateException("Bad redis script return");
            }

            long allowed = toLong(ret.get(0));
            long remaining = toLong(ret.get(1));
            long reset = toLong(ret.get(2));
            return new RateLimitResult(allowed == 1L, remaining, reset);

        } catch (Exception ex) {
            if (props.getFailPolicy() == FailPolicy.OPEN) {
                return new RateLimitResult(true, -1L, 0L);
            }
            throw new RateLimitException("RateLimit redis error, fail-close", ex);
        }
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(String.valueOf(o));
    }
}

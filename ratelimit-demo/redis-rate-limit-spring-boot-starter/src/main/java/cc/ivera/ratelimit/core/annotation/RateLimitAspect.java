package cc.ivera.ratelimit.core.annotation;

import cc.ivera.ratelimit.core.config.RateLimitProperties;
import cc.ivera.ratelimit.core.client.RateLimitResult;
import cc.ivera.ratelimit.core.exception.RateLimitException;
import cc.ivera.ratelimit.core.resolver.KeyResolver;
import cc.ivera.ratelimit.core.rule.FixedWindowRule;
import cc.ivera.ratelimit.core.rule.RateLimitRule;
import cc.ivera.ratelimit.core.rule.SlidingWindowRule;
import cc.ivera.ratelimit.core.rule.TokenBucketRule;
import cc.ivera.ratelimit.core.executor.RedisRateLimitExecutor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Aspect
public class RateLimitAspect {

    private final RateLimitProperties props;
    private final KeyResolver keyResolver;
    private final RedisRateLimitExecutor executor;

    public RateLimitAspect(RateLimitProperties props, KeyResolver keyResolver, RedisRateLimitExecutor executor) {
        this.props = props;
        this.keyResolver = keyResolver;
        this.executor = executor;
    }

    @Around("@annotation(ann)")
    public Object around(ProceedingJoinPoint pjp, RateLimit ann) throws Throwable {
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String expr = StringUtils.hasText(ann.key()) ? ann.key() : props.getDefaultKey();
        String rawKey = keyResolver.resolve(expr, pjp.getTarget(), method, pjp.getArgs());

        RateLimitRule rule = buildRule(ann);
        RateLimitResult r = executor.acquire(rawKey, rule);
        if (!r.isAllowed()) {
            throw new RateLimitException("Rate limited. remaining=" + r.getRemaining() + ", resetMillis=" + r.getResetMillis());
        }
        return pjp.proceed();
    }

    private RateLimitRule buildRule(RateLimit ann) {
        int permits = ann.permits() <= 0 ? 1 : ann.permits();
        switch (ann.strategy()) {
            case FIXED_WINDOW:
                return new FixedWindowRule(ann.limit(), ann.windowSeconds() * 1000L, permits);
            case SLIDING_WINDOW:
                return new SlidingWindowRule(ann.limit(), ann.windowSeconds() * 1000L, permits);
            case TOKEN_BUCKET:
                return new TokenBucketRule(ann.capacity(), ann.refillPerSecond(), permits);
            default:
                throw new IllegalArgumentException("Unknown strategy: " + ann.strategy());
        }
    }
}

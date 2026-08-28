package cc.ivera.ratelimit.core.annotation;

import cc.ivera.ratelimit.core.client.RateLimitStrategy;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    RateLimitStrategy strategy() default RateLimitStrategy.FIXED_WINDOW;

    /** SpEL key, e.g. 'login:' + #phone or 'ip:' + #request.getRemoteAddr() */
    String key() default "";

    int limit() default 10;
    int windowSeconds() default 1;

    int capacity() default 10;
    double refillPerSecond() default 5.0;

    int permits() default 1;
}

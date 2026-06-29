package com.example.vocab.quality;

import com.example.vocab.config.quality.QualityProperties;
import com.example.vocab.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class RequestGuardAspect {
    private final StringRedisTemplate redisTemplate;
    private final QualityProperties properties;

    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
        if (!Boolean.TRUE.equals(properties.getRateLimitEnabled())) return pjp.proceed();
        long limit = rateLimited.permitsPerMinute() > 0 ? rateLimited.permitsPerMinute() : properties.getRateLimitPerMinute();
        String key = "rl:" + CurrentUser.id() + ":" + rateLimited.key() + ":" + (System.currentTimeMillis() / 60000);
        Long count = redisTemplate.opsForValue().increment(key);
        if (Objects.equals(count, 1L)) redisTemplate.expire(key, Duration.ofMinutes(2));
        if (count != null && count > limit) throw new IllegalStateException("Too many requests, please retry later");
        return pjp.proceed();
    }

    @Around("@annotation(idempotent)")
    public Object idempotent(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        if (!Boolean.TRUE.equals(properties.getIdempotencyEnabled())) return pjp.proceed();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader(idempotent.header());
        if (!StringUtils.hasText(token)) return pjp.proceed();
        String key = "idem:" + CurrentUser.id() + ":" + request.getRequestURI() + ":" + token;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(properties.getIdempotencyExpireSeconds()));
        if (!Boolean.TRUE.equals(first)) throw new IllegalStateException("Duplicate request detected");
        return pjp.proceed();
    }
}

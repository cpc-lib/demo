package com.example.limit.aspect;

import com.example.limit.annotation.TokenBucketLimit;
import com.example.limit.exception.RateLimitException;
import com.example.limit.limiter.TokenBucketLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Aspect
@Component
public class TokenBucketAspect {
    @Resource
    TokenBucketLimiter limiter;

    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint p, TokenBucketLimit limit) throws Throwable {
        if (!limiter.tryAcquire(limit.key(), limit.rate(), limit.capacity()))
            throw new RateLimitException();
        return p.proceed();
    }
}

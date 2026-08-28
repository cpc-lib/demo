package com.example.limitdemo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(1)
public class ConcurrencyLimitAspect {

    private final RedissonClient redissonClient;

    public ConcurrencyLimitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint pjp, ConcurrencyLimit limit) throws Throwable {
        String key = "concurrency:" + limit.key();
        int maxPermits = limit.value();

        RSemaphore semaphore = redissonClient.getSemaphore(key);
        semaphore.trySetPermits(maxPermits);

        boolean acquired;

        try {
            if (limit.blocking()) {
                if (limit.timeoutMs() > 0) {
                    acquired = semaphore.tryAcquire(1, limit.timeoutMs(), TimeUnit.MILLISECONDS);
                    if (!acquired) {
                        throw new ConcurrencyLimitException("排队超时：" + limit.message());
                    }
                } else {
                    semaphore.acquire();
                }
            } else {
                acquired = semaphore.tryAcquire();
                if (!acquired) {
                    throw new ConcurrencyLimitException(limit.message());
                }
            }

            return pjp.proceed();
        } finally {
            if (semaphore.availablePermits() < maxPermits) {
                semaphore.release();
            }
        }
    }
}

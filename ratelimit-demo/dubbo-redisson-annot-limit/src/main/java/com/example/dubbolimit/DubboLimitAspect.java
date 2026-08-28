package com.example.dubbolimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class DubboLimitAspect {

    private final RedissonClient redissonClient;

    public DubboLimitAspect(RedissonClient client) {
        this.redissonClient = client;
    }

    @Around("@annotation(limit)")
    public Object around(ProceedingJoinPoint pjp, DubboLimit limit) throws Throwable {
        String key = "dubbo:limit:" + limit.key();
        RSemaphore sem = redissonClient.getSemaphore(key);
        sem.trySetPermits(limit.value());

        boolean ok;

        if (limit.blocking()) {
            if (limit.timeoutMs() > 0) {
                ok = sem.tryAcquire(1, limit.timeoutMs(), TimeUnit.MILLISECONDS);
                if (!ok) throw new DubboLimitException("排队超时：" + limit.message());
            } else {
                sem.acquire();
            }
        } else {
            ok = sem.tryAcquire();
            if (!ok) throw new DubboLimitException(limit.message());
        }

        try {
            return pjp.proceed();
        } finally {
            sem.release();
        }
    }
}

package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;


@Component
public class DistributedSemaphoreLimiter {
    private final RedissonClient redissonClient;
    private RSemaphore semaphore;
    private static final int MAX_PERMITS = 5;

    public DistributedSemaphoreLimiter(RedissonClient redissonClient){
        this.redissonClient = redissonClient;
    }

    @PostConstruct
    public void init(){
        semaphore = redissonClient.getSemaphore("external:concurrency:limit");
        semaphore.trySetPermits(MAX_PERMITS);
    }

    public boolean tryAcquire(){
        return semaphore.tryAcquire();
    }

    public void release(){
        semaphore.release();
    }
}

package com.example.distributedlock.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(
            @Value("${app.lock.redisson.address}") String address,
            @Value("${app.lock.redisson.password:}") String password,
            @Value("${app.lock.redisson.watchdog-timeout-ms:30000}") long watchdogTimeoutMs) {

        Config config = new Config();
        config.setLockWatchdogTimeout(watchdogTimeoutMs);
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password == null || password.isBlank() ? null : password)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(16);
        return Redisson.create(config);
    }
}

package com.example.fencing.config;

import java.util.Arrays;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${app.redis.mode}") String mode,
            @Value("${app.redis.nodes}") String nodeAddresses,
            @Value("${app.redis.password}") String password,
            @Value("${app.redis.watchdog-timeout-millis}") long watchdogTimeoutMillis) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("REDIS_PASSWORD must be configured");
        }
        if (watchdogTimeoutMillis < 1000) {
            throw new IllegalArgumentException("watchdog timeout must be >= 1000 ms");
        }
        String[] addresses = Arrays.stream(nodeAddresses.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).toArray(String[]::new);
        if (addresses.length == 0) {
            throw new IllegalArgumentException("APP_REDIS_NODES is empty");
        }

        Config config = new Config();
        config.setLockWatchdogTimeout(watchdogTimeoutMillis);
        if ("cluster".equalsIgnoreCase(mode)) {
            config.useClusterServers()
                    .addNodeAddress(addresses)
                    .setPassword(password)
                    // Docker Desktop NAT: Redis nodes announce bridge IPs (172.20.x.x)
                    // which are unreachable from Windows. Rewrite all node addresses
                    // back to 127.0.0.1 so Redisson connects through Docker port maps.
                    .setNatMapper(uri -> new org.redisson.misc.RedisURI(
                            uri.getScheme(), "127.0.0.1", uri.getPort()));
        } else if ("single".equalsIgnoreCase(mode)) {
            if (addresses.length != 1) {
                throw new IllegalArgumentException("single Redis mode needs exactly 1 address");
            }
            config.useSingleServer()
                    .setAddress(addresses[0])
                    .setPassword(password);
        } else {
            throw new IllegalArgumentException("APP_REDIS_MODE must be 'cluster' or 'single'");
        }
        return Redisson.create(config);
    }
}

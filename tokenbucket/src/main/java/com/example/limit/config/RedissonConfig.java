package com.example.limit.config;

import com.example.limit.limiter.TokenBucketLimiter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(
            @Value("${REDIS_HOST:127.0.0.1}") String host,
            @Value("${REDIS_PORT:6379}") int port,
            @Value("${REDIS_PASSWORD:}") String password) {
        Config config = new Config();
        config.setCodec(new StringCodec());
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(0);
        if (StringUtils.hasText(password)) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    public TokenBucketLimiter limiter(RedissonClient c) {
        return new TokenBucketLimiter(c);
    }
}

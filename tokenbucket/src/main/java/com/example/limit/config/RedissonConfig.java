package com.example.limit.config;

import com.example.limit.limiter.TokenBucketLimiter;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(new StringCodec());
        config.useSingleServer().setAddress("redis://192.168.220.200:6379").setDatabase(0).setPassword("cpc!23#@");
        return Redisson.create(config);
    }

    @Bean
    public TokenBucketLimiter limiter(RedissonClient c) {
        return new TokenBucketLimiter(c);
    }
}

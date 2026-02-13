package cc.ivera.ratelimit.core.config;

import cc.ivera.ratelimit.core.annotation.RateLimitAspect;
import cc.ivera.ratelimit.core.resolver.KeyResolver;
import cc.ivera.ratelimit.core.client.RateLimiterClient;
import cc.ivera.ratelimit.core.resolver.SpelKeyResolver;
import cc.ivera.ratelimit.core.executor.RedisRateLimitExecutor;
import cc.ivera.ratelimit.core.client.RedisScriptRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnClass(StringRedisTemplate.class)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisScriptRegistry redisScriptRegistry() {
        return new RedisScriptRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyResolver rateLimitKeyResolver() {
        return new SpelKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRateLimitExecutor redisRateLimitExecutor(StringRedisTemplate redis,
                                                        RedisScriptRegistry registry,
                                                        RateLimitProperties props) {
        return new RedisRateLimitExecutor(redis, registry, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterClient rateLimiterClient(RedisRateLimitExecutor executor) {
        return new RateLimiterClient(executor);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimitProperties props,
                                           KeyResolver resolver,
                                           RedisRateLimitExecutor executor) {
        return new RateLimitAspect(props, resolver, executor);
    }
}

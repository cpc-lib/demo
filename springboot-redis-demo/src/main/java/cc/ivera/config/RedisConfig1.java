package cc.ivera.config;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 自动缓存配置类： @EnableCachingn + @Cacheable()
 */
//@EnableCaching
//@Configuration
public class RedisConfig1 extends CachingConfigurerSupport {

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    /**
     * 自定义key生成策略
     *
     * @return
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            public Object generate(Object target, Method method, Object... objects) {
                StringBuilder sb = new StringBuilder();
                Cacheable annotation = method.getAnnotation(Cacheable.class);
                String[] cacheNames = annotation.cacheNames();
                for (String elem : cacheNames) {
                    sb.append(elem + ".");
                }
                sb.deleteCharAt(sb.length() - 1); // 删除掉 cacheNames 中的最后一个点(.)

                sb.append("::").append(target.getClass()
                        .getSimpleName()).append("::")
                        .append(method.getName()).append("::")
                        .append(Arrays.toString(objects));

                return sb.toString();
            }
        };
    }
}

package cc.ivera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 *  RedisTemplate手动缓存配置类（不进行配置就使用默认的配置）
 */
@Configuration
public class RedisConfig2 {

    @Bean
    public RedisTemplate<Integer,Integer> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<Integer,Integer> redisTemplate = new RedisTemplate<>();

        // 设置连接工厂类
        redisTemplate.setConnectionFactory(factory);

        // 设置k-v的序列化方式
        // Jackson2JsonRedisSerializer 实现了 RedisSerializer接口
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 还可设置很多的配置 ... ... (未设置就使用默认配置)

        return redisTemplate;
    }
}

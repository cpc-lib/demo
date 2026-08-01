package cc.ivera.ragdemo.config;


import cc.ivera.ragdemo.service.InMemoryIngestionTaskEventBus;
import cc.ivera.ragdemo.service.IngestionTaskEventBus;
import cc.ivera.ragdemo.service.RedisStreamIngestionTaskEventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class IngestionTaskEventBusConfig {

    private final RagProperties properties;

    @Bean
    public IngestionTaskEventBus ingestionTaskEventBus(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        String bus = properties.getIngestionEvents().getBus();
        if ("redis-stream".equalsIgnoreCase(bus)) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                return new RedisStreamIngestionTaskEventBus(properties, redisTemplate);
            }
        }
        return new InMemoryIngestionTaskEventBus(properties);
    }
}

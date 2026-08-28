package cc.ivera.userdemo.config;

import cc.ivera.userdemo.kafka.CacheWarmRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@org.springframework.context.annotation.Configuration
public class KafkaCacheWarmConfig {

  @Bean
  public KafkaTemplate<String, CacheWarmRequest> cacheWarmKafkaTemplate(org.springframework.core.env.Environment env) {
    String bootstrap = env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");

    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.RETRIES_CONFIG, 10);

    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, CacheWarmRequest> cacheWarmKafkaListenerContainerFactory(
      org.springframework.core.env.Environment env
  ) {
    String bootstrap = env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
    // 独立 groupId，避免和 ES 索引消费者混用
    String groupId = env.getProperty("app.kafka.group.cache-warm", "cache-warm-worker");

    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    JsonDeserializer<CacheWarmRequest> valueDeserializer = new JsonDeserializer<>(CacheWarmRequest.class);
    valueDeserializer.addTrustedPackages("*");

    DefaultKafkaConsumerFactory<String, CacheWarmRequest> cf = new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), valueDeserializer
    );

    ConcurrentKafkaListenerContainerFactory<String, CacheWarmRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf);
    factory.setConcurrency(2);
    return factory;
  }
}

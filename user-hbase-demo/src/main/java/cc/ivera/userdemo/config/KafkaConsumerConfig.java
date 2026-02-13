package cc.ivera.userdemo.config;

import cc.ivera.userdemo.event.UserEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@org.springframework.context.annotation.Configuration
public class KafkaConsumerConfig {

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, UserEvent> kafkaListenerContainerFactory(
      org.springframework.core.env.Environment env
  ) {
    String bootstrap = env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
    String groupId = env.getProperty("spring.kafka.consumer.group-id", "user-es-indexer");

    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    JsonDeserializer<UserEvent> valueDeserializer = new JsonDeserializer<>(UserEvent.class);
    valueDeserializer.addTrustedPackages("*");

    DefaultKafkaConsumerFactory<String, UserEvent> cf = new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), valueDeserializer
    );

    ConcurrentKafkaListenerContainerFactory<String, UserEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf);
    factory.setConcurrency(2);
    // 🔥 关键：开启手动 ack
    factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);
    return factory;
  }
}

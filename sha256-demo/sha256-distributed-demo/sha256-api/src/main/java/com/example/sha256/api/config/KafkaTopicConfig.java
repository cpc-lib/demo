package com.example.sha256.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "sha256.broker", havingValue = "kafka")
public class KafkaTopicConfig {
    @Bean
    NewTopic sha256Topic(@Value("${sha256.kafka.topic:sha256.tasks}") String topic,
                         @Value("${sha256.kafka.partitions:6}") int partitions) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(1).build();
    }
}

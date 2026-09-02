package com.example.sha256.worker.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@Configuration
@EnableKafkaRetryTopic
@ConditionalOnProperty(name = "sha256.broker", havingValue = "kafka")
public class KafkaRetryConfig {
}

package com.example.kafkaarticle.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic delay5sTopic() {
        return new NewTopic("article-delay-5s", 1, (short) 1);
    }

    @Bean
    public NewTopic delay30sTopic() {
        return new NewTopic("article-delay-30s", 1, (short) 1);
    }

    @Bean
    public NewTopic delay1mTopic() {
        return new NewTopic("article-delay-1m", 1, (short) 1);
    }

    @Bean
    public NewTopic delay5mTopic() {
        return new NewTopic("article-delay-5m", 1, (short) 1);
    }

    @Bean
    public NewTopic finalTopic() {
        return new NewTopic("article-delay-final", 1, (short) 1);
    }
}

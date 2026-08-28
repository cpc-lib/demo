package com.example.versionedcachemp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机 / 队列 / 绑定。
 */
@Configuration
public class MqConfig {

    public static final String EXCHANGE_USER = "user.cache.ex";
    public static final String QUEUE_USER_CACHE_UPDATE = "user.cache.update.q";
    public static final String ROUTING_USER_UPDATED = "user.update";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_USER, true, false);
    }

    @Bean
    public Queue userCacheUpdateQueue() {
        return new Queue(QUEUE_USER_CACHE_UPDATE, true);
    }

    @Bean
    public Binding userCacheUpdateBinding() {
        return BindingBuilder.bind(userCacheUpdateQueue())
                .to(userExchange())
                .with(ROUTING_USER_UPDATED);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

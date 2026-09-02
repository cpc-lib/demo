package com.example.sha256.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "sha256.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitTopologyConfig {
    @Bean
    DirectExchange sha256Exchange(@Value("${sha256.rabbit.exchange:sha256.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    DirectExchange sha256RetryExchange(@Value("${sha256.rabbit.retry-exchange:sha256.retry.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    DirectExchange sha256DeadLetterExchange(@Value("${sha256.rabbit.dlx:sha256.dlx}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue sha256Queue(@Value("${sha256.rabbit.queue:sha256.tasks}") String name,
                      @Value("${sha256.rabbit.dlx:sha256.dlx}") String dlx,
                      @Value("${sha256.rabbit.dlq-routing-key:sha256.dead}") String dlqRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }

    @Bean
    Queue sha256RetryQueue(@Value("${sha256.rabbit.retry-queue:sha256.tasks.retry}") String name,
                           @Value("${sha256.rabbit.retry-delay-ms:5000}") long retryDelayMs,
                           @Value("${sha256.rabbit.exchange:sha256.exchange}") String mainExchange,
                           @Value("${sha256.rabbit.routing-key:sha256.calculate}") String routingKey) {
        return QueueBuilder.durable(name)
                .ttl((int) Math.min(Integer.MAX_VALUE, retryDelayMs))
                .deadLetterExchange(mainExchange)
                .deadLetterRoutingKey(routingKey)
                .build();
    }

    @Bean
    Queue sha256DeadLetterQueue(@Value("${sha256.rabbit.dlq:sha256.tasks.dlq}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    Binding sha256Binding(Queue sha256Queue,
                          DirectExchange sha256Exchange,
                          @Value("${sha256.rabbit.routing-key:sha256.calculate}") String routingKey) {
        return BindingBuilder.bind(sha256Queue).to(sha256Exchange).with(routingKey);
    }

    @Bean
    Binding sha256RetryBinding(Queue sha256RetryQueue,
                               DirectExchange sha256RetryExchange,
                               @Value("${sha256.rabbit.retry-routing-key:sha256.retry}") String routingKey) {
        return BindingBuilder.bind(sha256RetryQueue).to(sha256RetryExchange).with(routingKey);
    }

    @Bean
    Binding sha256DeadLetterBinding(Queue sha256DeadLetterQueue,
                                    DirectExchange sha256DeadLetterExchange,
                                    @Value("${sha256.rabbit.dlq-routing-key:sha256.dead}") String routingKey) {
        return BindingBuilder.bind(sha256DeadLetterQueue).to(sha256DeadLetterExchange).with(routingKey);
    }
}

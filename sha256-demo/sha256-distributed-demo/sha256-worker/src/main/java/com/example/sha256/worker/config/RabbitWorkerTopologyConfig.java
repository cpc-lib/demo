package com.example.sha256.worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "sha256.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitWorkerTopologyConfig {
    @Bean
    DirectExchange sha256Exchange(@Value("${sha256.rabbit.exchange:sha256.exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    Queue sha256Queue(@Value("${sha256.rabbit.queue:sha256.tasks}") String name) {
        return new Queue(name, true);
    }

    @Bean
    Binding sha256Binding(Queue sha256Queue,
                          DirectExchange sha256Exchange,
                          @Value("${sha256.rabbit.routing-key:sha256.calculate}") String routingKey) {
        return BindingBuilder.bind(sha256Queue).to(sha256Exchange).with(routingKey);
    }
}

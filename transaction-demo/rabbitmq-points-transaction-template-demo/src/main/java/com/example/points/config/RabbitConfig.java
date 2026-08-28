package com.example.points.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String REGISTER_EXCHANGE = "user.register.exchange";
    public static final String REGISTER_QUEUE = "user.register.queue";
    public static final String REGISTER_ROUTING_KEY = "user.register.routing";

    @Bean
    public DirectExchange registerExchange() {
        return new DirectExchange(REGISTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue registerQueue() {
        return QueueBuilder.durable(REGISTER_QUEUE).build();
    }

    @Bean
    public Binding registerBinding(@Qualifier("registerQueue") Queue queue,
                                   @Qualifier("registerExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(REGISTER_ROUTING_KEY);
    }
}

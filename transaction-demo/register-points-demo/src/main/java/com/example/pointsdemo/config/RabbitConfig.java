package com.example.pointsdemo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String REGISTER_EXCHANGE = "register.points.exchange";
    public static final String REGISTER_QUEUE = "register.points.queue";
    public static final String REGISTER_ROUTING_KEY = "register.points.key";

    @Bean
    public Exchange registerExchange() {
        return ExchangeBuilder.directExchange(REGISTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue registerQueue() {
        return QueueBuilder.durable(REGISTER_QUEUE).build();
    }

    @Bean
    public Binding registerBinding() {
        return BindingBuilder.bind(registerQueue()).to(registerExchange()).with(REGISTER_ROUTING_KEY).noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
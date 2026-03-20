package com.example.points.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_REGISTER = "user.register.ex";
    public static final String QUEUE_REGISTER = "user.register.q";
    public static final String ROUTING_REGISTER = "user.reg";

    @Bean
    public DirectExchange userRegisterExchange() {
        return new DirectExchange(EXCHANGE_REGISTER, true, false);
    }

    @Bean
    public Queue userRegisterQueue() {
        return new Queue(QUEUE_REGISTER, true);
    }

    @Bean
    public Binding userRegisterBinding(Queue userRegisterQueue, DirectExchange userRegisterExchange) {
        return BindingBuilder.bind(userRegisterQueue).to(userRegisterExchange).with(ROUTING_REGISTER);
    }


    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

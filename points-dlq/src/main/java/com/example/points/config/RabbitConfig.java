package com.example.points.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** ================== 业务队列（v2） ================== */

    public static final String USER_REGISTER_EXCHANGE = "user.register.v3.ex";
    public static final String USER_REGISTER_QUEUE = "user.register.v3.q";
    public static final String USER_REGISTER_ROUTING_KEY = "user.register.v2";

    /** ================== 死信队列（DLX） ================== */

    public static final String USER_REGISTER_DLX_EXCHANGE = "user.register.v3.dlx.ex";
    public static final String USER_REGISTER_DLX_QUEUE = "user.register.v3.dlx.q";
    public static final String USER_REGISTER_DLX_ROUTING_KEY = "user.register.v3.dlx";

    /** ================== 交换机声明 ================== */

    @Bean
    public DirectExchange userRegisterExchange() {
        return new DirectExchange(USER_REGISTER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange userRegisterDlxExchange() {
        return new DirectExchange(USER_REGISTER_DLX_EXCHANGE, true, false);
    }

    /** ================== 队列声明 ================== */

    /**
     * 业务队列：必须绑定死信交换机
     */
    @Bean
    public Queue userRegisterQueue() {
        return QueueBuilder.durable(USER_REGISTER_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_REGISTER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_REGISTER_DLX_ROUTING_KEY)
                .build();
    }

    /**
     * 死信队列：无须再绑定 DLX
     */
    @Bean
    public Queue userRegisterDlxQueue() {
        return QueueBuilder.durable(USER_REGISTER_DLX_QUEUE).build();
    }

    /** ================== 队列绑定 ================== */

    @Bean
    public Binding userRegisterBinding() {
        return BindingBuilder.bind(userRegisterQueue())
                .to(userRegisterExchange())
                .with(USER_REGISTER_ROUTING_KEY);
    }

    @Bean
    public Binding userRegisterDlxBinding() {
        return BindingBuilder.bind(userRegisterDlxQueue())
                .to(userRegisterDlxExchange())
                .with(USER_REGISTER_DLX_ROUTING_KEY);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

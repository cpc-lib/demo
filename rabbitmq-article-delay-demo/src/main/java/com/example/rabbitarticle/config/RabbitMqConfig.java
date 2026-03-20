package com.example.rabbitarticle.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String DELAY_EXCHANGE = "article.delay.exchange";
    public static final String DELAY_QUEUE = "article.delay.queue";
    public static final String DELAY_ROUTING_KEY = "article.delay.routing";

    public static final String PUBLISH_EXCHANGE = "article.publish.exchange";
    public static final String PUBLISH_QUEUE = "article.publish.queue";
    public static final String PUBLISH_ROUTING_KEY = "article.publish.routing";

    /**
     * 延迟交换机（普通 Direct）
     */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /**
     * 发布交换机（死信目标交换机）
     */
    @Bean
    public DirectExchange publishExchange() {
        return new DirectExchange(PUBLISH_EXCHANGE);
    }

    /**
     * 延迟队列：设置死信交换机和路由键
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        // TTL 到期后消息会作为死信转发到 publishExchange
        args.put("x-dead-letter-exchange", PUBLISH_EXCHANGE);
        args.put("x-dead-letter-routing-key", PUBLISH_ROUTING_KEY);
        return new Queue(DELAY_QUEUE, true, false, false, args);
    }

    /**
     * 最终执行发布的队列
     */
    @Bean
    public Queue publishQueue() {
        return new Queue(PUBLISH_QUEUE, true);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(delayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding publishBinding() {
        return BindingBuilder.bind(publishQueue())
                .to(publishExchange())
                .with(PUBLISH_ROUTING_KEY);
    }
}

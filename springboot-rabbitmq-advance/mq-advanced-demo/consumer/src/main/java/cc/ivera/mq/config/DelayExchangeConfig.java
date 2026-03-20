package cc.ivera.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayExchangeConfig {

    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder.directExchange("delay.direct").delayed()//设置延时交换机
                .durable(true).build();
    }

    @Bean
    public Queue delayQueue() {
        // 使用QueueBuilder构建队列，durable就是持久化的
        return QueueBuilder.durable("delay.queue").build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with("delay");
    }
}

package cc.ivera.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//消费者启动可以创建路由与队列
@Configuration
public class CommonConfig {
    @Bean
    public DirectExchange simpleExchange() {
        // 三个参数：交换机名称、是否持久化、当没有queue与其绑定时是否自动删除
        return new DirectExchange("simple.direct", true, false);
    }

    @Bean
    public Queue simpleQueue() {
        // 使用QueueBuilder构建队列，durable就是持久化的
        return QueueBuilder.durable("simple.queue").build();
    }

    @Bean
    public Binding simpleBinding() {
        return BindingBuilder.bind(simpleQueue()).to(simpleExchange()).with("simple.test");
    }
}

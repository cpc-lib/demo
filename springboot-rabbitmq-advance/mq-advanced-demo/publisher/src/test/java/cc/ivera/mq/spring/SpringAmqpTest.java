package cc.ivera.mq.spring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
//集群模式仲裁队列
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAmqpTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //confirm
    @Test
    public void testSendMessage2SimpleQueue() throws InterruptedException {
        // 1.消息体
        String message = "hello, spring amqp!";
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 3.添加callback
        correlationData.getFuture().addCallback(result -> {
            if (result.isAck()) {
                // 3.1.ack，消息成功
                log.debug("消息发送成功, ID:{}", correlationData.getId());
            } else {
                // 3.2.nack，消息失败
                log.error("消息发送失败, ID:{}, 原因{}", correlationData.getId(), result.getReason());
            }
        }, ex -> log.error("消息发送异常, ID:{}, 原因{}", correlationData.getId(), ex.getMessage()));
        // 4.发送消息(在rabbitmq中的web端创建给路由添加binding)
        rabbitTemplate.convertAndSend("simple.direct", "simple.test", message, correlationData);

        // 休眠一会儿，等待ack回执
        Thread.sleep(2000);
    }


    @Test
    public void testDurableMessage() throws InterruptedException {
        // 1.消息体
        Message message = MessageBuilder.withBody("Hello,durable message".getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 4.发送消息(在rabbitmq中的web端创建给路由添加binding)
        rabbitTemplate.convertAndSend("simple.direct", "simple.test", message, correlationData);
    }

    @Test
    public void testAutoack() throws InterruptedException {
        // 1.消息体
        Message message = MessageBuilder.withBody("Hello, message".getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 4.发送消息(在rabbitmq中的web端创建给路由添加binding)
        rabbitTemplate.convertAndSend("autoack.direct", "autoack.test", message, correlationData);
    }


    //基于dlx实现延时信息
    @Test
    public void testTtlMessage() throws InterruptedException {
        // 1.消息体（信息的延时采用短的时间 队列延时 信息延时）
        Message message = MessageBuilder.withBody("Hello,ttl message".getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).setExpiration("5000").build();
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 4.发送消息(在rabbitmq中的web端创建给路由添加binding)
        rabbitTemplate.convertAndSend("ttl.direct", "ttl", message, correlationData);
        log.info("发送信息成功:{}", correlationData.getId());
    }


    @Test
    public void testDelayExchange() throws InterruptedException {
        // 1.消息体（信息的延时采用短的时间 队列延时 信息延时）
        Message message = MessageBuilder.withBody("Hello,delayed message".getBytes(StandardCharsets.UTF_8)).setHeader("x-delay", 10000).build();//设置延时
        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        // 4.发送消息(在rabbitmq中的web端创建给路由添加binding)
        rabbitTemplate.convertAndSend("delay.direct", "delay", message, correlationData);
        log.info("发送信息成功:{}", correlationData.getId());
    }
}

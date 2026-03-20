package cc.ivera.ttl.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import cc.ivera.ttl.config.v3.DelayQueueConfig;

import java.util.Date;

/**
 * 队列ttl的消费者
 */
@Component
@Slf4j
public class DeadLetterQueueListener {


    @RabbitListener(queues = "QD")
    public void receiveD(Message msg) {
        log.info("当前时间:{},收到死信队列的消息:{}", new Date(), msg.getBody());
        System.out.println(new String(msg.getBody()));
    }



    @RabbitListener(queues = DelayQueueConfig.DELAYED_QUEUE_NAME)
    public void receiveDelayQueue(Message msg) {
        String message = new String(msg.getBody());
        log.info("当前时间:{},收到延迟队列的消息:{}", new Date(), message);
    }
}

package cc.ivera.mq.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringRabbitListener {

    //不带确认的简单监听 设置确认机制为none,直接确认消息到达
    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueue(String msg) {
        log.info("消费者接收到simple.queue的消息：【{}】", msg);
        // 模拟异常
        System.out.println(1 / 0);
        log.debug("消息处理完成！");
    }

    //设置确认机制为auto,报错后不会确认,不设置重试次数，重入队列
    @RabbitListener(queues = "autoack.queue")
    public void listenAutoackQueue(String msg) {
        log.info("消费者接收到autoack.queue的消息：【{}】", msg);
        // 模拟异常
        System.out.println(1 / 0);
        log.debug("消息处理完成！");
    }

    //基于dlx实现延时消息 dealletterexchange,deadroutingkey,queue
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "dl.ttl.queue", durable = "true"), exchange = @Exchange(name = "dl.ttl.direct"), key = "dl"))
    public void listenDlQueue(String msg) {
        log.info("接收到 dl.ttl.queue的延迟消息：{}", msg);
    }

    //使用延时插件delayexchange实现延时信息
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "delay.queue", durable = "true"),
            //设置delay为true 不设置会报错
            exchange = @Exchange(name = "delay.direct", delayed = "true"), key = "delay"))
    public void listenDelayQueue(String msg) {
        log.info("接收到delay.queue的延迟消息：{}", msg);
    }
}

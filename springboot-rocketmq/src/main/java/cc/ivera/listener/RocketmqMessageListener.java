package cc.ivera.listener;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic="order_id",consumerGroup="${rocketmq.producer.group}")
public class RocketmqMessageListener implements RocketMQListener<String> {
    @Override
    public void onMessage(String id) {
        System.out.println("正在处理消息id:"+id);
    }
}

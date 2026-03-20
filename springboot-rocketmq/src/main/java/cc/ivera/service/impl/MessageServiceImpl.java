package cc.ivera.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import cc.ivera.service.MessageService;

@Service
@Slf4j
@Primary
public class MessageServiceImpl implements MessageService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void sendMessage(String id) {
        System.out.println("发送消息到rocketmq id:" + id);
        rocketMQTemplate.asyncSend("order_id", id, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("处理成功!");
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println("处理失败");
            }
        });

    }

    @Override
    public String doMessage() {
        return null;
    }
}

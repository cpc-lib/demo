package cc.ivera.topic.controller;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReceiveAndReplyMessageCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.topic.config.TopicRabbitConfig;
import cc.ivera.topic.domain.User;

import javax.annotation.Resource;

@RestController
public class SendMessageController {

    @Resource
    RabbitTemplate rabbitTemplate;


    @GetMapping("/topic")
    public String topic() {

        User user=new User("1", "张三", 12);
        rabbitTemplate.convertAndSend(TopicRabbitConfig.EXCHANGE_NAME,
                TopicRabbitConfig.ROUTING_KEY, user);

        //定义回调
        rabbitTemplate.receiveAndReply(TopicRabbitConfig.QUEUE_NAME, new ReceiveAndReplyMessageCallback() {
            @Override
            public Message handle(Message payload) {
                System.out.println(payload);
                return null;
            }
        });

        return "ok";
    }

}

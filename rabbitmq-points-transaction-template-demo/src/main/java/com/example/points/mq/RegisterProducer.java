package com.example.points.mq;

import com.alibaba.fastjson2.JSONObject;
import com.example.points.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterProducer {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("[Producer] Message NOT reached exchange, cause: " + cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("[Producer] Message NOT routed to queue: " + returned.getMessage());
        });
    }

    public void sendRegisterMsg(Long userId) {
        String msgId = UUID.randomUUID().toString();

        JSONObject json = new JSONObject();
        json.put("msgId", msgId);
        json.put("userId", userId);

        Message message = MessageBuilder.withBody(json.toJSONString().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(msgId).setTimestamp(new Date()).build();

        rabbitTemplate.send(RabbitConfig.REGISTER_EXCHANGE, RabbitConfig.REGISTER_ROUTING_KEY, message);


        System.out.println("[Producer] Sent register message, msgId=" + msgId + ", userId=" + userId);
    }
}

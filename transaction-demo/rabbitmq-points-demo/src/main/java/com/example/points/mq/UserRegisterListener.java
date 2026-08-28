package com.example.points.mq;

import com.example.points.dto.UserRegisterMsg;
import com.example.points.service.PointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.integration.IntegrationProperties;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisterListener {

    private final ObjectMapper objectMapper;
    private final PointsService pointsService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "user.register.q", ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();

        String content = new String(message.getBody());
        UserRegisterMsg msg = objectMapper.readValue(content, UserRegisterMsg.class);

        log.info("收到注册消息: userId={}, businessId={}", msg.getUserId(), msg.getBusinessId());

        try {
            pointsService.processRegisterEvent(msg);

            // 业务成功 → 正常 ACK
            channel.basicAck(tag, false);

        } catch (Exception ex) {

            // 自己维护重试次数
            Integer retry = message.getMessageProperties().getHeader("x-retry-count");
            if (retry == null) retry = 0;

            log.error("注册消息处理失败，userId={}, retry={}, err={}", 
                    msg.getUserId(), retry, ex.getMessage());

            if (retry < 3) {

                // 设置新的重试次数
                message.getMessageProperties().setHeader("x-retry-count", retry + 1);

                // ⭐ 使用 RabbitTemplate 重发消息（正确 API）
                rabbitTemplate.convertAndSend(
                        message.getMessageProperties().getReceivedExchange(),
                        message.getMessageProperties().getReceivedRoutingKey(),
                        message
                );

                // ACK 当前旧消息
                channel.basicAck(tag, false);

            } else {
                log.error("消息重试 > 3 次，写入人工处理表 businessId={}", msg.getBusinessId());
                pointsService.saveToManual(msg.getBusinessId(),msg.getUserId(),ex.getMessage());

                // 最终 ACK（不再重试）
                channel.basicAck(tag, false);


                // 第 4 次（>3）失败：拒绝并不重新入队 → 进入死信队列
                //channel.basicNack(tag, false, false);
            }
        }
    }
}

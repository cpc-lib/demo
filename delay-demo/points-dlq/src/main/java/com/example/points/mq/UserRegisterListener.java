package com.example.points.mq;

import com.example.points.config.RabbitConfig;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.service.PointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 业务队列消费：失败重试 3 次，超过进入 DLX
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisterListener {

    private final ObjectMapper objectMapper;
    private final PointsService pointsService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitConfig.USER_REGISTER_QUEUE, ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();
        String content = new String(message.getBody());
        UserRegisterMsg msg = objectMapper.readValue(content, UserRegisterMsg.class);

        try {
            pointsService.processRegisterEvent(msg);
            channel.basicAck(tag, false);
        } catch (Exception ex) {

            Integer retry = message.getMessageProperties().getHeader("x-retry-count");
            if (retry == null) {
                retry = 0;
            }

            log.error("处理注册消息失败 userId={}, retry={}, err={}",
                    msg.getUserId(), retry, ex.getMessage());

            if (retry < 3) {

                Integer finalRetry = retry;
                rabbitTemplate.convertAndSend(
                        RabbitConfig.USER_REGISTER_EXCHANGE,
                        RabbitConfig.USER_REGISTER_ROUTING_KEY,
                        message.getBody(),
                        m -> {
                            m.getMessageProperties().getHeaders().putAll(
                                    message.getMessageProperties().getHeaders()
                            );
                            m.getMessageProperties().setHeader("x-retry-count", finalRetry + 1);
                            m.getMessageProperties().setContentType(
                                    message.getMessageProperties().getContentType());
                            return m;
                        }
                );

                channel.basicAck(tag, false);

            } else {
                log.error("消息重试超过3次，进入死信队列 businessId={}", msg.getBusinessId());
                channel.basicNack(tag, false, false);
            }
        }
    }
}

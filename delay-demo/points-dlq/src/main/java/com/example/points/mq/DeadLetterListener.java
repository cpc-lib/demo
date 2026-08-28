package com.example.points.mq;

import com.example.points.config.RabbitConfig;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.service.ManualProcessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列监听：写入人工处理表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterListener {

    private final ObjectMapper objectMapper;
    private final ManualProcessService manualProcessService;

    @RabbitListener(queues = RabbitConfig.USER_REGISTER_DLX_QUEUE)
    public void onDeadMessage(Message message) throws Exception {

        String content = new String(message.getBody());
        UserRegisterMsg msg = objectMapper.readValue(content, UserRegisterMsg.class);

        log.error("死信队列收到消息，写入人工处理表 userId={}, businessId={}",
                msg.getUserId(), msg.getBusinessId());

        manualProcessService.saveManualRecord(
                msg.getBusinessId(),
                msg.getUserId(),
                "超过最大重试次数，进入死信队列"
        );
    }
}

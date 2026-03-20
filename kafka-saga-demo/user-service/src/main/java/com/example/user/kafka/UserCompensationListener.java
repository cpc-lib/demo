package com.example.user.kafka;

import com.example.common.events.UserRegisterCompensateEvent;
import com.example.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCompensationListener {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    @KafkaListener(topics = "user.register.compensate", groupId = "user-service-compensate-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        String value = record.value();
        try {
            UserRegisterCompensateEvent event = objectMapper.readValue(value, UserRegisterCompensateEvent.class);
            log.info("收到补偿事件: {}", event);
            userService.handleUserRegisterCompensation(event);
        } catch (Exception e) {
            log.error("处理补偿事件失败, value={}", value, e);
        }
    }
}

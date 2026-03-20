package com.example.user.kafka;

import com.example.common.events.UserRegisteredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventProducer {

    public static final String USER_REGISTERED_TOPIC = "user.registered.event";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(USER_REGISTERED_TOPIC, event.getEventId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 UserRegisteredEvent 失败", e);
        }
    }
}

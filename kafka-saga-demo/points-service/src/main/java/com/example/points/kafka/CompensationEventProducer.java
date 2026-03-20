package com.example.points.kafka;

import com.example.common.events.UserRegisterCompensateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationEventProducer {

    public static final String USER_REGISTER_COMPENSATE_TOPIC = "user.register.compensate";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCompensationEvent(UserRegisterCompensateEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(USER_REGISTER_COMPENSATE_TOPIC, event.getEventId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 UserRegisterCompensateEvent 失败", e);
        }
    }
}

package com.example.points.kafka;

import com.example.common.events.UserRegisteredEvent;
import com.example.common.events.UserRegisterCompensateEvent;
import com.example.points.service.CompensationService;
import com.example.points.service.PointsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final ObjectMapper objectMapper;
    private final PointsService pointsService;
    private final CompensationService compensationService;

    @KafkaListener(topics = "user.registered.event", groupId = "points-service-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        String value = record.value();
        try {
            UserRegisteredEvent event = objectMapper.readValue(value, UserRegisteredEvent.class);
            log.info("收到用户注册事件: {}", event);
            pointsService.handleUserRegistered(event);
        } catch (Exception e) {
            log.error("处理用户注册事件失败，触发补偿, value={}", value, e);
            try {
                UserRegisteredEvent event = objectMapper.readValue(value, UserRegisteredEvent.class);
                UserRegisterCompensateEvent compensateEvent =
                        compensationService.createCompensationEvent(event.getUserId(), event.getEventId(), e.getMessage());
                log.info("已创建补偿事件: {}", compensateEvent.getEventId());
            } catch (Exception ex) {
                log.error("创建补偿事件失败, value={}", value, ex);
            }
        }
    }
}

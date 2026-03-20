package com.example.user.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.events.UserRegisteredEvent;
import com.example.user.entity.SagaEventLog;
import com.example.user.kafka.UserEventProducer;
import com.example.user.mapper.SagaEventLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final SagaEventLogMapper sagaEventLogMapper;
    private final ObjectMapper objectMapper;
    private final UserEventProducer userEventProducer;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        List<SagaEventLog> logs = sagaEventLogMapper.selectList(
                Wrappers.<SagaEventLog>lambdaQuery()
                        .eq(SagaEventLog::getStatus, 0)
                        .eq(SagaEventLog::getEventType, "USER_REGISTERED")
                        .last("limit 50")
        );

        for (SagaEventLog logEntity : logs) {
            try {
                UserRegisteredEvent event = objectMapper.readValue(logEntity.getPayload(), UserRegisteredEvent.class);
                userEventProducer.sendUserRegisteredEvent(event);
                logEntity.setStatus(1);
                sagaEventLogMapper.updateById(logEntity);
                log.info("Outbox 发布 USER_REGISTERED 事件: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Outbox 发布事件失败, eventId={}", logEntity.getEventId(), e);
                logEntity.setStatus(2);
                sagaEventLogMapper.updateById(logEntity);
            }
        }
    }
}

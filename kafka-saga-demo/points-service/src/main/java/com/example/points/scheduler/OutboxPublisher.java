package com.example.points.scheduler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.events.UserRegisterCompensateEvent;
import com.example.points.entity.SagaEventLog;
import com.example.points.kafka.CompensationEventProducer;
import com.example.points.mapper.SagaEventLogMapper;
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
    private final CompensationEventProducer compensationEventProducer;

    @Scheduled(fixedDelay = 5000)
    public void publishOutboxEvents() {
        // 这里只处理补偿事件的 Outbox
        List<SagaEventLog> logs = sagaEventLogMapper.selectList(
                Wrappers.<SagaEventLog>lambdaQuery()
                        .eq(SagaEventLog::getStatus, 0)
                        .eq(SagaEventLog::getEventType, "USER_REGISTER_COMPENSATE")
                        .last("limit 50")
        );

        for (SagaEventLog logEntity : logs) {
            try {
                UserRegisterCompensateEvent event = objectMapper.readValue(logEntity.getPayload(), UserRegisterCompensateEvent.class);
                compensationEventProducer.sendCompensationEvent(event);
                logEntity.setStatus(1);
                sagaEventLogMapper.updateById(logEntity);
                log.info("Outbox 发布 USER_REGISTER_COMPENSATE 事件: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Outbox 发布补偿事件失败, eventId={}", logEntity.getEventId(), e);
                logEntity.setStatus(2);
                sagaEventLogMapper.updateById(logEntity);
            }
        }
    }
}

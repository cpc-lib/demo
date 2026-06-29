package com.example.vocab.service.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.entity.outbox.OutboxEvent;
import com.example.vocab.mapper.outbox.OutboxEventMapper;
import com.example.vocab.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventMapper mapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Long append(String aggregateType, Long aggregateId, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        e.setPayload(payload);
        e.setStatus("PENDING");
        e.setRetryCount(0);
        e.setNextRetryTime(LocalDateTime.now());
        mapper.insert(e);
        return e.getId();
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:5000}")
    public void dispatchPending() {
        List<OutboxEvent> events = mapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                .in(OutboxEvent::getStatus, "PENDING", "FAILED")
                .le(OutboxEvent::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(OutboxEvent::getId)
                .last("LIMIT 50"));
        for (OutboxEvent event : events) dispatchOne(event);
    }

    public List<OutboxEvent> list(String status) {
        return mapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                .eq(OutboxEvent::getStatus, status)
                .orderByDesc(OutboxEvent::getId)
                .last("LIMIT 100"));
    }

    public void dispatchOne(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.DOMAIN_EXCHANGE, event.getEventType(), event.getPayload());
            event.setStatus("SENT");
            event.setErrorMessage(null);
            mapper.updateById(event);
        } catch (Exception ex) {
            int retry = event.getRetryCount() == null ? 0 : event.getRetryCount();
            event.setRetryCount(retry + 1);
            event.setStatus("FAILED");
            event.setErrorMessage(ex.getMessage());
            event.setNextRetryTime(LocalDateTime.now().plusSeconds(Math.min(300, (long) Math.pow(2, Math.min(8, retry)))));
            mapper.updateById(event);
        }
    }
}

package com.example.points.service;

import com.example.common.events.UserRegisterCompensateEvent;
import com.example.points.entity.SagaEventLog;
import com.example.points.mapper.SagaEventLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompensationService {

    private final SagaEventLogMapper sagaEventLogMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserRegisterCompensateEvent createCompensationEvent(Long userId, String originEventId, String reason) {
        String eventId = UUID.randomUUID().toString();
        UserRegisterCompensateEvent event = new UserRegisterCompensateEvent();
        event.setEventId(eventId);
        event.setUserId(userId);
        event.setReason(reason);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            payload = "{}";
        }

        SagaEventLog logEntity = new SagaEventLog();
        logEntity.setEventId(eventId);
        logEntity.setEventType("USER_REGISTER_COMPENSATE");
        logEntity.setPayload(payload);
        logEntity.setStatus(0);
        sagaEventLogMapper.insert(logEntity);

        return event;
    }
}

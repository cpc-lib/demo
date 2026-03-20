package com.example.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.events.UserRegisteredEvent;
import com.example.common.events.UserRegisterCompensateEvent;
import com.example.user.entity.SagaEventLog;
import com.example.user.entity.User;
import com.example.user.mapper.SagaEventLogMapper;
import com.example.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final SagaEventLogMapper sagaEventLogMapper;
    private final ObjectMapper objectMapper;

    private static final int REGISTER_BONUS_POINTS = 100;

    @Transactional
    public Long registerUser(String username, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);

        String eventId = UUID.randomUUID().toString();
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEventId(eventId);
        event.setUserId(user.getId());
        event.setUsername(user.getUsername());
        event.setPhone(user.getPhone());
        event.setInitPoints(REGISTER_BONUS_POINTS);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化事件失败", e);
        }

        SagaEventLog log = new SagaEventLog();
        log.setEventId(eventId);
        log.setEventType("USER_REGISTERED");
        log.setPayload(payload);
        log.setStatus(0);
        sagaEventLogMapper.insert(log);

        return user.getId();
    }

    @Transactional
    public void handleUserRegisterCompensation(UserRegisterCompensateEvent event) {
        // 幂等判断
        SagaEventLog existed = sagaEventLogMapper.selectOne(
                Wrappers.<SagaEventLog>lambdaQuery()
                        .eq(SagaEventLog::getEventId, event.getEventId())
                        .eq(SagaEventLog::getEventType, "USER_REGISTER_COMPENSATE")
        );
        if (existed != null && existed.getStatus() != null && existed.getStatus() == 1) {
            return;
        }

        // 补偿逻辑：把用户状态改为 0
        User user = userMapper.selectById(event.getUserId());
        if (user != null) {
            user.setStatus(0);
            userMapper.updateById(user);
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            payload = "{}";
        }

        if (existed == null) {
            existed = new SagaEventLog();
            existed.setEventId(event.getEventId());
            existed.setEventType("USER_REGISTER_COMPENSATE");
            existed.setPayload(payload);
        }
        existed.setStatus(1);
        sagaEventLogMapper.insert(existed);
    }
}

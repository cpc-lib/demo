package com.example.points.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.events.UserRegisteredEvent;
import com.example.points.entity.PointsAccount;
import com.example.points.entity.SagaEventLog;
import com.example.points.mapper.PointsAccountMapper;
import com.example.points.mapper.SagaEventLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsAccountMapper pointsAccountMapper;
    private final SagaEventLogMapper sagaEventLogMapper;

    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        // 幂等检查：是否已成功处理过
        Long count = sagaEventLogMapper.selectCount(
                Wrappers.<SagaEventLog>lambdaQuery()
                        .eq(SagaEventLog::getEventId, event.getEventId())
                        .eq(SagaEventLog::getEventType, "USER_REGISTERED")
                        .eq(SagaEventLog::getStatus, 1)
        );
        if (count != null && count > 0) {
            log.info("事件 {} 已经处理过，跳过", event.getEventId());
            return;
        }

        // 模拟业务异常: 用户名包含 "fail" 时，抛异常触发补偿
        if (event.getUsername() != null && event.getUsername().contains("fail")) {
            throw new RuntimeException("模拟积分发放失败，触发补偿");
        }

        PointsAccount account = pointsAccountMapper.selectOne(
                Wrappers.<PointsAccount>lambdaQuery().eq(PointsAccount::getUserId, event.getUserId())
        );
        if (account == null) {
            account = new PointsAccount();
            account.setUserId(event.getUserId());
            account.setPoints(event.getInitPoints());
            account.setVersion(0);
            pointsAccountMapper.insert(account);
        } else {
            account.setPoints(account.getPoints() + event.getInitPoints());
            pointsAccountMapper.updateById(account);
        }

        // 记录已处理成功
        SagaEventLog logEntity = new SagaEventLog();
        logEntity.setEventId(event.getEventId());
        logEntity.setEventType("USER_REGISTERED");
        logEntity.setPayload("handled");
        logEntity.setStatus(1);
        sagaEventLogMapper.insert(logEntity);

        log.info("成功为用户 {} 发放注册送积分 {} 分", event.getUserId(), event.getInitPoints());
    }
}

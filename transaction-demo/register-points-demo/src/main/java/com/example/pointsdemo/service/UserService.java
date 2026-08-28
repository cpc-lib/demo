package com.example.pointsdemo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.pointsdemo.entity.PointsLog;
import com.example.pointsdemo.entity.User;
import com.example.pointsdemo.mapper.PointsLogMapper;
import com.example.pointsdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PointsLogMapper pointsLogMapper;
    private final TransactionTemplate transactionTemplate;

    private static final int REGISTER_POINTS = 100;

    /**
     * 用户注册：新增用户 + 记录积分消息日志（待发送）
     */
    public void register(User user) {
        transactionTemplate.execute(status -> {
            try {
                user.setCreateTime(new Date());
                userMapper.insert(user);

                PointsLog logEntity = new PointsLog();
                logEntity.setUserId(user.getId());
                logEntity.setBusinessId(UUID.randomUUID().toString());
                logEntity.setPoints(REGISTER_POINTS);
                logEntity.setStatus(0); // 待发送
                logEntity.setRetryCount(0);
                logEntity.setCreateTime(new Date());
                pointsLogMapper.insert(logEntity);

                log.info("注册成功 userId={}, businessId={}", user.getId(), logEntity.getBusinessId());
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
            return null;
        });
    }
}
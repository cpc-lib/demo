package com.example.points.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.entity.PointsLog;
import com.example.points.entity.UserPoints;
import com.example.points.mapper.PointsLogMapper;
import com.example.points.mapper.UserPointsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final UserPointsMapper userPointsMapper;
    private final ManualProcessService manualProcessService;
    private final TransactionTemplate transactionTemplate;

    private static final int REGISTER_POINTS = 100;

    /**
     * 正常处理注册送积分（失败抛异常；业务事务）
     */
    public void processRegisterEvent(UserRegisterMsg msg) {

        transactionTemplate.execute(status -> {

            try {
                PointsLog logEntity = pointsLogMapper.selectOne(new QueryWrapper<PointsLog>().eq("business_id", msg.getBusinessId()));

                // 幂等：已成功
                if (logEntity != null && logEntity.getStatus() == 1) {
                    log.info("businessId={} 已处理成功，跳过", msg.getBusinessId());
                    return null;
                }

                // 首次创建日志
                if (logEntity == null) {
                    logEntity = new PointsLog();
                    logEntity.setUserId(msg.getUserId());
                    logEntity.setBusinessId(msg.getBusinessId());
                    logEntity.setPoints(REGISTER_POINTS);
                    logEntity.setStatus(0);
                    logEntity.setRetryCount(0);
                    pointsLogMapper.insert(logEntity);
                }

                // 调内部独立事务
                Boolean ok = addUserPointsTx(msg.getUserId(), REGISTER_POINTS);
                if (!ok) {
                    throw new RuntimeException("积分更新失败");
                }

                logEntity.setStatus(1);
                logEntity.setRetryCount(logEntity.getRetryCount() + 1);
                logEntity.setErrMsg(null);
                pointsLogMapper.updateById(logEntity);

                log.info("送积分成功 userId={}, businessId={}", msg.getUserId(), msg.getBusinessId());

                return null;

            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    /**
     * 独立内部事务（强制新事务）
     */
    private Boolean addUserPointsTx(Long userId, Integer points) {
        return transactionTemplate.execute(status -> {
            try {
                UserPoints up = userPointsMapper.selectById(userId);

                if (up == null) {
                    up = new UserPoints();
                    up.setUserId(userId);
                    up.setPoints(points);
                    userPointsMapper.insert(up);
                    return false;
                } else {
                    up.setPoints(up.getPoints() + points);
                    return userPointsMapper.updateById(up) == 1;
                }

            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    /**
     * 写入人工处理表
     */
    public void saveToManual(String bussinessId, Long userId, String error) {
        manualProcessService.saveManualRecord(bussinessId, userId, error, REGISTER_POINTS);
    }
}

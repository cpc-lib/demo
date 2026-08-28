package com.example.pointsdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.pointsdemo.entity.PointsLog;
import com.example.pointsdemo.entity.UserPoints;
import com.example.pointsdemo.mapper.PointsLogMapper;
import com.example.pointsdemo.mapper.UserPointsMapper;
import com.example.pointsdemo.model.RegisterPointsMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final UserPointsMapper userPointsMapper;
    private final PointsLogMapper pointsLogMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 处理积分增加消息（幂等）
     */
    public void process(RegisterPointsMsg msg) {
        AtomicReference<Boolean> flag = new AtomicReference<>(false);
        AtomicReference<String> message = new AtomicReference<>("");
        transactionTemplate.execute(status -> {
            try {
                // 幂等：根据 businessId 查询
                PointsLog logEntity = pointsLogMapper.selectOne(
                        new QueryWrapper<PointsLog>().eq("business_id", msg.getBusinessId())
                );

                if (logEntity == null) {
                    // 理论上不会发生：兜底创建一条日志
                    logEntity = new PointsLog();
                    logEntity.setUserId(msg.getUserId());
                    logEntity.setBusinessId(msg.getBusinessId());
                    logEntity.setPoints(msg.getPoints());
                    logEntity.setStatus(1); // 已发送
                    logEntity.setRetryCount(0);
                    logEntity.setCreateTime(new Date());
                    pointsLogMapper.insert(logEntity);
                } else if (logEntity.getStatus() != null && logEntity.getStatus() == 2) {
                    log.info("businessId={} 已经处理成功，跳过", msg.getBusinessId());
                    return null;
                }

                // 用户积分加分
                UserPoints up = userPointsMapper.selectById(msg.getUserId());
                if (up == null) {
                    up = new UserPoints();
                    up.setUserId(msg.getUserId());
                    up.setPoints(msg.getPoints());
                    userPointsMapper.insert(up);
                } else {
                    up.setPoints(up.getPoints() + msg.getPoints());
                    userPointsMapper.updateById(up);
                }

                // 更新日志状态为已消费
                logEntity.setStatus(2);
                logEntity.setRetryCount(logEntity.getRetryCount() + 1);
                logEntity.setErrMsg(null);
                logEntity.setUpdateTime(new Date());
                pointsLogMapper.updateById(logEntity);

                log.info("增加积分成功 userId={}, businessId={}", msg.getUserId(), msg.getBusinessId());
                int i = 1 / 0;
            } catch (Exception e) {
                status.setRollbackOnly();
                flag.set(true);
                message.set(e.getMessage());
                throw e;
            } finally {
                Boolean val = flag.get();
                if (val) {
//                    // 更新失败状态
//                    PointsLog logEntity = pointsLogMapper.selectOne(
//                            new QueryWrapper<PointsLog>().eq("business_id", msg.getBusinessId())
//                    );
//                    if (logEntity != null) {
//                        logEntity.setStatus(3);
//                        logEntity.setRetryCount((logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount()) + 1);
//                        logEntity.setErrMsg(message.get());
//                        logEntity.setUpdateTime(new Date());
//                        pointsLogMapper.updateById(logEntity);
//                    }
                }
            }
            return null;
        });
    }
}
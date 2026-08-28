package com.example.points.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.entity.PointsLog;
import com.example.points.service.ManualProcessService;
import com.example.points.service.PointsService;
import com.example.points.mapper.PointsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsRetryTask {

    private final PointsLogMapper pointsLogMapper;
    private final PointsService pointsService;
    private final ManualProcessService manualProcessService;

    @Scheduled(fixedDelay = 60000)
    public void retryFailed() {
        List<PointsLog> list = pointsLogMapper.selectList(
                new QueryWrapper<PointsLog>().eq("status", 2)
        );

        if (list.isEmpty()) {
            return;
        }

        log.info("开始重试送积分，失败记录数={}", list.size());

        for (PointsLog logEntity : list) {
            if (logEntity.getRetryCount() > 5) {
                manualProcessService.saveManualRecord(logEntity.getBusinessId(), logEntity.getUserId(), "重试超过次数", logEntity.getPoints());
                continue;
            }

            try {
                pointsService.processRegisterEvent(
                        new UserRegisterMsg(logEntity.getUserId(), logEntity.getBusinessId())
                );
            } catch (Exception e) {
                // 已在 service 中记录日志，这里略过
            }
        }
    }
}

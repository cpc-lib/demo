package com.example.pointsdemo.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.pointsdemo.config.RabbitConfig;
import com.example.pointsdemo.entity.PointsLog;
import com.example.pointsdemo.mapper.PointsLogMapper;
import com.example.pointsdemo.model.RegisterPointsMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsLogScanTask {

    private final PointsLogMapper pointsLogMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 扫描待发送积分日志，发送 MQ 消息
     */
    @Scheduled(fixedDelay = 5000)
    public void scanAndSend() {
        List<PointsLog> list = pointsLogMapper.selectList(
                new QueryWrapper<PointsLog>().eq("status", 0)
        );

        for (PointsLog logEntity : list) {
            RegisterPointsMsg msg = new RegisterPointsMsg();
            msg.setUserId(logEntity.getUserId());
            msg.setBusinessId(logEntity.getBusinessId());
            msg.setPoints(logEntity.getPoints());

            rabbitTemplate.convertAndSend(
                    RabbitConfig.REGISTER_EXCHANGE,
                    RabbitConfig.REGISTER_ROUTING_KEY,
                    msg
            );

            logEntity.setStatus(1); // 已发送
            logEntity.setSendTime(new Date());
            logEntity.setUpdateTime(new Date());
            pointsLogMapper.updateById(logEntity);

            log.info("发送积分消息成功 businessId={}", logEntity.getBusinessId());
        }
    }
}
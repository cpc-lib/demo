package com.example.pointsdemo.mq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.pointsdemo.entity.PointsLog;
import com.example.pointsdemo.mapper.PointsLogMapper;
import com.example.pointsdemo.model.RegisterPointsMsg;
import com.example.pointsdemo.service.PointsService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterPointsConsumer {

    private final PointsService pointsService;

    private final RabbitTemplate rabbitTemplate;

    private final PointsLogMapper pointsLogMapper;


    @RabbitListener(queues = "register.points.queue", ackMode = "MANUAL")
    public void onMessage(RegisterPointsMsg msg, Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            pointsService.process(msg);
            channel.basicAck(tag, false);
            return;
        } catch (Exception e) {

            // 1. 从 header 获取 retry-count
            Integer retry = message.getMessageProperties().getHeader("x-retry-count");
            if (retry == null) {
                retry = 0;
            }

            log.error("消费失败 businessId={}, retry={}, err={}", msg.getBusinessId(), retry, e.getMessage());

            if (retry < 3) {
                // 2. 设置新的 header，再次入队
                message.getMessageProperties().setHeader("x-retry-count", retry + 1);

                // 让 Spring 重新发送消息
                // 这里不能直接 requeue=true，否则 header 会丢失
                rabbitTemplate.send("register.points.exchange", "register.points.key", message);

                // 当前消息 ACK（因为已经手动把新消息发回队列）
                channel.basicAck(tag, false);

            } else {
                // 更新失败状态
                PointsLog logEntity = pointsLogMapper.selectOne(new QueryWrapper<PointsLog>().eq("business_id", msg.getBusinessId()));
                if (logEntity != null) {
                    logEntity.setStatus(3);
                    logEntity.setRetryCount((logEntity.getRetryCount() == null ? 0 : retry));
                    logEntity.setErrMsg("超过重试次数");
                    logEntity.setUpdateTime(new Date());
                    pointsLogMapper.updateById(logEntity);
                }
                // 重试超过 3 次 → 丢弃
                channel.basicAck(tag, false);
            }
        }
    }


}
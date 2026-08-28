package com.example.points.mq;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.points.config.RabbitConfig;
import com.example.points.entity.DeadMessage;
import com.example.points.entity.MsgIdempotent;
import com.example.points.entity.UserPoints;
import com.example.points.mapper.DeadMessageMapper;
import com.example.points.mapper.MsgIdempotentMapper;
import com.example.points.mapper.UserPointsMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class RegisterConsumer {

    private final UserPointsMapper pointsMapper;
    private final MsgIdempotentMapper idempotentMapper;
    private final DeadMessageMapper deadMessageMapper;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "user.register.queue")
    public void onMessage(Message msg, Channel channel) throws IOException {
        String msgId = msg.getMessageProperties().getMessageId();
        Date timestamp = msg.getMessageProperties().getTimestamp();
        System.out.println("msgId = " + msgId);
        String body = new String(msg.getBody());
        JSONObject json = JSON.parseObject(body);
        Long userId = json.getLong("userId");
        long tag = msg.getMessageProperties().getDeliveryTag();
        try {
            doBiz(msgId, userId);
            //只接受这么一条信息
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            // 简单重试计数在 header 中
            Integer retry = msg.getMessageProperties().getHeader("x-retry-count");
            retry = retry == null ? 0 : retry;
            if (retry < 3) {
                Message message = MessageBuilder.withBody(json.toJSONString().getBytes(StandardCharsets.UTF_8)).copyHeaders(msg.getMessageProperties().getHeaders()).setContentType(MessageProperties.CONTENT_TYPE_JSON).setMessageId(msgId).setTimestamp(timestamp).setHeader("x-retry-count", retry + 1).build();
                rabbitTemplate.send(RabbitConfig.REGISTER_EXCHANGE, RabbitConfig.REGISTER_ROUTING_KEY, message);
            } else {
                // 超过 3 次：写入人工处理表，并确认 ack，避免堆积
                DeadMessage dm = new DeadMessage();
                dm.setMsgId(msgId);
                dm.setUserId(userId);
                dm.setPayload(body);
                dm.setReason(ex.getMessage());
                dm.setCreateTime(new Date());
                deadMessageMapper.insert(dm);
                channel.basicAck(tag, false);
            }

        }
    }


    public void doBiz(String msgId, Long userId) {
        // 2. 编程式事务
        transactionTemplate.execute(status -> {
            try {
                // 2.1 再次检查（事务内防并发）
                MsgIdempotent again = idempotentMapper.selectById(msgId);
                if (again != null) {
                    return null;
                }
                // 2.2 业务：增加积分
                UserPoints p = new UserPoints();
                p.setUserId(userId);
                p.setPoints(100);
                p.setReason("REGISTER");
                p.setCreateTime(new Date());
                pointsMapper.insert(p);

                // 2.3 幂等记录写入（必须有 UNIQUE(msg_id)）
                MsgIdempotent record = new MsgIdempotent(msgId, 1, new Date());
                idempotentMapper.insert(record);

            } catch (Exception e) {
                status.setRollbackOnly(); // 回滚事务
                throw e;
            }
            return null;
        });
    }
}

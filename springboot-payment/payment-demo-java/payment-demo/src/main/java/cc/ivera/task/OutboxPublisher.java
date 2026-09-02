package cc.ivera.task;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.entity.MessageOutbox;
import cc.ivera.mapper.MessageOutboxMapper;
import cc.ivera.mq.OrderCloseMessage;
import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.mq.RefundStatusSyncMessage;
import cc.ivera.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OutboxPublisher {

    private final MessageOutboxMapper mapper;

    private final RabbitTemplate rabbitTemplate;

    @Value("${payment.outbox.batch-size:50}")
    private int batchSize = 50;

    @Value("${payment.outbox.lease-seconds:30}")
    private long leaseSeconds = 30L;

    @Value("${payment.outbox.confirm-timeout-ms:5000}")
    private long confirmTimeoutMillis = 5000L;

    @Value("${payment.outbox.retry-base-ms:1000}")
    private long retryBaseMillis = 1000L;

    @Value("${payment.outbox.retry-max-ms:60000}")
    private long retryMaxMillis = 60000L;

    @Value("${payment.outbox.max-attempts:5}")
    private int maxAttempts = 5;

    public OutboxPublisher(MessageOutboxMapper mapper, RabbitTemplate rabbitTemplate) {
        this.mapper = mapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        List<MessageOutbox> candidates = mapper.selectPublishable(Math.max(1, batchSize));
        if (candidates == null) {
            candidates = Collections.emptyList();
        }
        for (MessageOutbox event : candidates) {
            publishIfClaimed(event);
        }
    }

    private void publishIfClaimed(MessageOutbox event) {
        if (event == null || event.getId() == null) {
            return;
        }
        String claimToken = UUID.randomUUID().toString();
        if (mapper.claimForPublish(event.getId(), claimToken, Math.max(1L, leaseSeconds)) != 1) {
            return;
        }

        try {
            publishAndAwaitConfirm(event, claimToken);
            if (mapper.markSent(event.getId(), claimToken, new Date()) != 1) {
                throw new IllegalStateException("Outbox发布确认已收到，但消息租约已变化");
            }
        } catch (Exception exception) {
            Date nextRetryTime = nextRetryTime(event);
            mapper.markFailed(
                    event.getId(),
                    claimToken,
                    abbreviate(exception.getMessage()),
                    nextRetryTime
            );
            log.error("Outbox消息发布失败，eventId={}, eventType={}",
                    event.getEventId(), event.getEventType(), exception);
        }
    }

    private void publishAndAwaitConfirm(MessageOutbox event, String claimToken) throws Exception {
        RoutedMessage routed = route(event);
        CorrelationData correlation = new CorrelationData(event.getEventId() + ":" + claimToken);
        rabbitTemplate.convertAndSend(
                routed.exchange,
                routed.routingKey,
                routed.payload,
                correlation
        );
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(Math.max(1L, confirmTimeoutMillis), TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            String reason = confirm == null ? "RabbitMQ未返回发布确认" : confirm.getReason();
            throw new IllegalStateException("RabbitMQ发布未确认: " + reason);
        }
        if (correlation.getReturnedMessage() != null) {
            throw new IllegalStateException("RabbitMQ消息无法路由");
        }
    }

    private RoutedMessage route(MessageOutbox event) {
        Map<String, Object> payload = JsonUtils.toObjectMap(event.getPayload());
        if (OutboxEventTypes.ORDER_CLOSE_SCHEDULED.equals(event.getEventType())) {
            OrderCloseMessage message = new OrderCloseMessage();
            message.setEventId(required(event.getEventId(), "关单事件ID不能为空"));
            message.setOrderNo(required(value(payload, "orderNo"), "关单订单号不能为空"));
            message.setPaymentType(required(value(payload, "paymentType"), "关单支付类型不能为空"));
            return new RoutedMessage(
                    OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE,
                    OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY,
                    message
            );
        }
        if (OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED.equals(event.getEventType())) {
            RefundStatusSyncMessage message = new RefundStatusSyncMessage();
            message.setEventId(required(event.getEventId(), "退款状态同步事件ID不能为空"));
            message.setRefundNo(required(value(payload, "refundNo"), "退款单号不能为空"));
            return new RoutedMessage(
                    RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE,
                    RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY,
                    message
            );
        }
        throw new IllegalArgumentException("不支持的Outbox事件类型: " + event.getEventType());
    }

    private Date nextRetryTime(MessageOutbox event) {
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();
        int nextAttempt = retryCount + 1;
        if (nextAttempt >= Math.max(1, maxAttempts)) {
            return null;
        }
        int exponent = Math.min(retryCount, 20);
        long factor = 1L << exponent;
        long delay;
        try {
            delay = Math.multiplyExact(Math.max(1L, retryBaseMillis), factor);
        } catch (ArithmeticException overflow) {
            delay = retryMaxMillis;
        }
        delay = Math.min(delay, Math.max(1L, retryMaxMillis));
        return new Date(System.currentTimeMillis() + delay);
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : value.toString();
    }

    private String required(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String abbreviate(String message) {
        String value = StringUtils.hasText(message) ? message : "Outbox消息发布失败";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private static final class RoutedMessage {

        private final String exchange;

        private final String routingKey;

        private final Object payload;

        private RoutedMessage(String exchange, String routingKey, Object payload) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.payload = payload;
        }
    }
}

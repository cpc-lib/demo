package cc.ivera.mq;

import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.RefundStatus;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.RefundStatusSyncMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class RefundStatusSyncConsumer {

    private static final String CONSUMER_NAME = "refund-status-sync";

    private final RefundApplicationService refundApplicationService;

    private final MessageConsumeLogService consumeLogService;

    private final RefundStatusSyncMessageService statusSyncMessageService;

    private final TransactionTemplate transactionTemplate;

    public RefundStatusSyncConsumer(
            RefundApplicationService refundApplicationService,
            MessageConsumeLogService consumeLogService,
            RefundStatusSyncMessageService statusSyncMessageService,
            TransactionTemplate transactionTemplate
    ) {
        this.refundApplicationService = refundApplicationService;
        this.consumeLogService = consumeLogService;
        this.statusSyncMessageService = statusSyncMessageService;
        this.transactionTemplate = transactionTemplate;
    }

    @RabbitListener(queues = RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_RELEASE_QUEUE)
    public void handleRefundStatusSync(RefundStatusSyncMessage message) {
        if (message == null || !StringUtils.hasText(message.getRefundNo())) {
            log.warn("Received empty refund status sync message, ignored");
            return;
        }

        String refundNo = message.getRefundNo();
        int attempt = message.getAttempt() == null ? 0 : message.getAttempt();
        if (attempt < 0) {
            throw retryLater("退款状态同步次数无效", null);
        }
        String businessKey = refundNo + ":" + attempt;
        String eventId = resolveEventId(message, businessKey);
        MessageConsumeClaim claim;
        try {
            claim = consumeLogService.tryStart(
                    eventId,
                    CONSUMER_NAME,
                    OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED,
                    businessKey
            );
        } catch (RuntimeException exception) {
            throw retryLater("退款状态同步消息认领失败", exception);
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.CONSUMED) {
            return;
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.BUSY) {
            throw retryLater("退款状态同步消息正在处理，请稍后重试", null);
        }

        try {
            RefundInfo refundInfo = refundApplicationService.queryRefundStatus(refundNo);
            transactionTemplate.execute(status -> {
                if (!isTerminal(refundInfo)) {
                    statusSyncMessageService.sendRefundStatusSyncMessage(
                            refundNo,
                            Math.incrementExact(attempt)
                    );
                }
                consumeLogService.complete(eventId, CONSUMER_NAME, claim.getLeaseToken());
                return null;
            });
        } catch (RuntimeException exception) {
            consumeLogService.fail(
                    eventId,
                    CONSUMER_NAME,
                    claim.getLeaseToken(),
                    exception.getMessage(),
                    null
            );
            throw retryLater("退款状态同步消息处理失败", exception);
        }
    }

    private boolean isTerminal(RefundInfo refundInfo) {
        if (refundInfo == null || !StringUtils.hasText(refundInfo.getRefundStatus())) {
            return false;
        }
        String status = refundInfo.getRefundStatus();
        return RefundStatus.SUCCESS.getType().equals(status)
                || RefundStatus.CLOSED.getType().equals(status);
    }

    private String resolveEventId(RefundStatusSyncMessage message, String businessKey) {
        if (StringUtils.hasText(message.getEventId()) && message.getEventId().length() <= 36) {
            return message.getEventId();
        }
        String identity = OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED + ":" + businessKey;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private AmqpRejectAndDontRequeueException retryLater(String message, RuntimeException cause) {
        if (cause == null) {
            return new AmqpRejectAndDontRequeueException(message);
        }
        return new AmqpRejectAndDontRequeueException(message, cause);
    }
}


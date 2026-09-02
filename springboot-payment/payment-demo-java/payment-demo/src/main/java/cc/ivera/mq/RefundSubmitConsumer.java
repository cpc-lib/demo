package cc.ivera.mq;

import cc.ivera.config.RefundSubmitRabbitConfig;
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
public class RefundSubmitConsumer {

    private static final String CONSUMER_NAME = "refund-submit";

    private final RefundApplicationService refundApplicationService;

    private final MessageConsumeLogService consumeLogService;

    private final RefundStatusSyncMessageService statusSyncMessageService;

    private final TransactionTemplate transactionTemplate;

    public RefundSubmitConsumer(
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

    @RabbitListener(queues = RefundSubmitRabbitConfig.REFUND_SUBMIT_QUEUE)
    public void handleRefundSubmit(RefundSubmitMessage message) {
        if (message == null || !StringUtils.hasText(message.getRefundNo())) {
            log.warn("收到空退款提交消息，忽略处理");
            return;
        }

        String refundNo = message.getRefundNo();
        String eventId = resolveEventId(message, refundNo);
        MessageConsumeClaim claim;
        try {
            claim = consumeLogService.tryStart(
                    eventId,
                    CONSUMER_NAME,
                    OutboxEventTypes.REFUND_SUBMIT_REQUESTED,
                    refundNo
            );
        } catch (RuntimeException exception) {
            throw retryLater("退款提交消息认领失败", exception);
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.CONSUMED) {
            log.info("退款提交消息已处理，忽略重复投递，eventId={}, refundNo={}", eventId, refundNo);
            return;
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.BUSY) {
            throw retryLater("退款提交消息正在处理，请稍后重试", null);
        }

        try {
            refundApplicationService.submitApprovedRefund(refundNo);
            transactionTemplate.execute(status -> {
                statusSyncMessageService.sendRefundStatusSyncMessage(refundNo, 0);
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
            throw retryLater("退款提交消息处理失败", exception);
        }
    }

    private String resolveEventId(RefundSubmitMessage message, String refundNo) {
        if (StringUtils.hasText(message.getEventId()) && message.getEventId().length() <= 36) {
            return message.getEventId();
        }
        String identity = OutboxEventTypes.REFUND_SUBMIT_REQUESTED + ":" + refundNo;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private AmqpRejectAndDontRequeueException retryLater(String message, RuntimeException cause) {
        if (cause == null) {
            return new AmqpRejectAndDontRequeueException(message);
        }
        return new AmqpRejectAndDontRequeueException(message, cause);
    }
}

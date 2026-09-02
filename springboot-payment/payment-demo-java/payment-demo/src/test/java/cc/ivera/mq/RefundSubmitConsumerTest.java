package cc.ivera.mq;

import cc.ivera.config.RefundSubmitRabbitConfig;
import cc.ivera.exception.BizException;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.RefundStatusSyncMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Queue;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefundSubmitConsumerTest {

    private RefundApplicationService refundApplicationService;
    private MessageConsumeLogService consumeLogService;
    private RefundStatusSyncMessageService statusSyncMessageService;
    private TransactionTemplate transactionTemplate;
    private RefundSubmitConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        refundApplicationService = mock(RefundApplicationService.class);
        consumeLogService = mock(MessageConsumeLogService.class);
        statusSyncMessageService = mock(RefundStatusSyncMessageService.class);
        transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        consumer = new RefundSubmitConsumer(
                refundApplicationService,
                consumeLogService,
                statusSyncMessageService,
                transactionTemplate
        );
    }

    @Test
    void claimedSubmissionCallsTheChannelThenAtomicallySchedulesStatusSyncAndCompletesInbox() {
        when(consumeLogService.tryStart(
                "event-1", "refund-submit",
                OutboxEventTypes.REFUND_SUBMIT_REQUESTED, "REFUND-1"))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));

        consumer.handleRefundSubmit(message("event-1", "REFUND-1"));

        verify(refundApplicationService).submitApprovedRefund("REFUND-1");
        verify(statusSyncMessageService).sendRefundStatusSyncMessage("REFUND-1", 0);
        verify(consumeLogService).complete("event-1", "refund-submit", "lease-1");
        verify(transactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void consumedSubmissionDoesNotCallTheChannelAgain() {
        when(consumeLogService.tryStart(
                "event-1", "refund-submit",
                OutboxEventTypes.REFUND_SUBMIT_REQUESTED, "REFUND-1"))
                .thenReturn(MessageConsumeClaim.consumed());

        consumer.handleRefundSubmit(message("event-1", "REFUND-1"));

        verifyNoInteractions(refundApplicationService, statusSyncMessageService, transactionTemplate);
    }

    @Test
    void busySubmissionIsRejectedToTheRetryQueue() {
        when(consumeLogService.tryStart(
                "event-1", "refund-submit",
                OutboxEventTypes.REFUND_SUBMIT_REQUESTED, "REFUND-1"))
                .thenReturn(MessageConsumeClaim.busy());

        assertThatThrownBy(() -> consumer.handleRefundSubmit(message("event-1", "REFUND-1")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("正在处理");

        verifyNoInteractions(refundApplicationService, statusSyncMessageService, transactionTemplate);
    }

    @Test
    void channelFailureMarksInboxFailedAndRejectsToTheRetryQueue() {
        when(consumeLogService.tryStart(
                "event-1", "refund-submit",
                OutboxEventTypes.REFUND_SUBMIT_REQUESTED, "REFUND-1"))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));
        doThrow(new BizException("channel timeout"))
                .when(refundApplicationService).submitApprovedRefund("REFUND-1");

        assertThatThrownBy(() -> consumer.handleRefundSubmit(message("event-1", "REFUND-1")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("退款提交消息处理失败");

        verify(consumeLogService).fail(
                eq("event-1"), eq("refund-submit"), eq("lease-1"),
                org.mockito.ArgumentMatchers.contains("channel timeout"), isNull());
        verify(statusSyncMessageService, never()).sendRefundStatusSyncMessage(anyString(), any(Integer.class));
    }

    @Test
    void legacySubmissionMessageGetsAStableUuidIdentity() {
        when(consumeLogService.tryStart(
                anyString(), eq("refund-submit"),
                eq(OutboxEventTypes.REFUND_SUBMIT_REQUESTED), eq("REFUND-1")))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));

        consumer.handleRefundSubmit(message(null, "REFUND-1"));

        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        verify(consumeLogService).tryStart(
                eventId.capture(), eq("refund-submit"),
                eq(OutboxEventTypes.REFUND_SUBMIT_REQUESTED), eq("REFUND-1"));
        assertThat(eventId.getValue()).hasSize(36);
    }

    @Test
    void rejectedSubmissionsTravelThroughABoundedDelayQueue() {
        RefundSubmitRabbitConfig config = new RefundSubmitRabbitConfig();
        Queue submitQueue = config.refundSubmitQueue();
        Queue retryQueue = config.refundSubmitRetryQueue(1000L);

        assertThat(submitQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", RefundSubmitRabbitConfig.REFUND_SUBMIT_RETRY_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RefundSubmitRabbitConfig.REFUND_SUBMIT_RETRY_ROUTING_KEY);
        assertThat(retryQueue.getArguments())
                .containsEntry("x-message-ttl", 1000L)
                .containsEntry("x-dead-letter-exchange", RefundSubmitRabbitConfig.REFUND_SUBMIT_EVENT_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RefundSubmitRabbitConfig.REFUND_SUBMIT_ROUTING_KEY);
    }

    private RefundSubmitMessage message(String eventId, String refundNo) {
        RefundSubmitMessage message = new RefundSubmitMessage();
        message.setEventId(eventId);
        message.setRefundNo(refundNo);
        return message;
    }
}

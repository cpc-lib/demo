package cc.ivera.mq;

import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.RefundStatus;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.RefundStatusSyncMessageService;
import cc.ivera.service.impl.RefundStatusSyncMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Queue;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

class RefundStatusSyncMessagingTest {

    private RefundApplicationService refundApplicationService;
    private MessageConsumeLogService consumeLogService;
    private RefundStatusSyncMessageService statusSyncMessageService;
    private TransactionTemplate transactionTemplate;
    private RefundStatusSyncConsumer consumer;

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
        consumer = new RefundStatusSyncConsumer(
                refundApplicationService,
                consumeLogService,
                statusSyncMessageService,
                transactionTemplate
        );
    }

    @Test
    void statusSyncServiceWritesAttemptedOutboxEventInsteadOfSendingRabbitDirectly() {
        MessageOutboxService outboxService = mock(MessageOutboxService.class);
        RefundStatusSyncMessageService service = new RefundStatusSyncMessageServiceImpl(outboxService);

        service.sendRefundStatusSyncMessage("REFUND-1", 3);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).insertOnce(
                eq("REFUND_STATUS_SYNC_REQUESTED:REFUND-1:3"),
                eq("REFUND"),
                eq("REFUND-1"),
                eq(OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED),
                payload.capture()
        );
        assertThat(payload.getValue()).contains("\"refundNo\":\"REFUND-1\"");
        assertThat(payload.getValue()).contains("\"attempt\":3");
    }

    @Test
    void oneArgumentStatusSyncEntryDelegatesToAttemptZero() {
        MessageOutboxService outboxService = mock(MessageOutboxService.class);
        RefundStatusSyncMessageService service = new RefundStatusSyncMessageServiceImpl(outboxService);

        service.sendRefundStatusSyncMessage("REFUND-1");

        verify(outboxService).insertOnce(
                eq("REFUND_STATUS_SYNC_REQUESTED:REFUND-1:0"),
                eq("REFUND"),
                eq("REFUND-1"),
                eq(OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED),
                anyString()
        );
    }

    @Test
    void claimedNonTerminalStatusSchedulesNextAttemptAndCompletesInboxInOneShortTransaction() {
        when(consumeLogService.tryStart(
                "event-1", "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:2"))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));
        when(refundApplicationService.queryRefundStatus("REFUND-1"))
                .thenReturn(refund("REFUND-1", RefundStatus.PROCESSING));

        consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2));

        verify(refundApplicationService).queryRefundStatus("REFUND-1");
        verify(statusSyncMessageService).sendRefundStatusSyncMessage("REFUND-1", 3);
        verify(consumeLogService).complete("event-1", "refund-status-sync", "lease-1");
        verify(transactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void claimedTerminalStatusCompletesInboxWithoutSchedulingAnotherAttempt() {
        when(consumeLogService.tryStart(
                "event-1", "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:2"))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));
        when(refundApplicationService.queryRefundStatus("REFUND-1"))
                .thenReturn(refund("REFUND-1", RefundStatus.SUCCESS));

        consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2));

        verify(statusSyncMessageService, never()).sendRefundStatusSyncMessage(anyString(), any(Integer.class));
        verify(consumeLogService).complete("event-1", "refund-status-sync", "lease-1");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = RefundStatus.class, names = {"FAILED", "ABNORMAL"})
    void retryableFailureKeepsQueryingInsteadOfLosingTheRefundObligation(RefundStatus status) {
        when(consumeLogService.tryStart(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));
        when(refundApplicationService.queryRefundStatus("REFUND-1"))
                .thenReturn(refund("REFUND-1", status));

        consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2));

        verify(statusSyncMessageService).sendRefundStatusSyncMessage("REFUND-1", 3);
        verify(consumeLogService).complete("event-1", "refund-status-sync", "lease-1");
    }

    @Test
    void consumedStatusSyncDoesNotQueryTheChannelAgain() {
        when(consumeLogService.tryStart(
                "event-1", "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:2"))
                .thenReturn(MessageConsumeClaim.consumed());

        consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2));

        verifyNoInteractions(refundApplicationService, statusSyncMessageService, transactionTemplate);
    }

    @Test
    void busyStatusSyncIsRejectedForDelayedRedelivery() {
        when(consumeLogService.tryStart(
                "event-1", "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:2"))
                .thenReturn(MessageConsumeClaim.busy());

        assertThatThrownBy(() -> consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("正在处理");

        verifyNoInteractions(refundApplicationService, statusSyncMessageService, transactionTemplate);
    }

    @Test
    void queryFailureMarksInboxFailedAndRejectsForDelayedRedelivery() {
        when(consumeLogService.tryStart(
                "event-1", "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:2"))
                .thenReturn(MessageConsumeClaim.claimed("lease-1"));
        doThrow(new IllegalStateException("channel timeout"))
                .when(refundApplicationService).queryRefundStatus("REFUND-1");

        assertThatThrownBy(() -> consumer.handleRefundStatusSync(message("event-1", "REFUND-1", 2)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("退款状态同步消息处理失败");

        verify(consumeLogService).fail(
                eq("event-1"), eq("refund-status-sync"), eq("lease-1"),
                org.mockito.ArgumentMatchers.contains("channel timeout"), isNull());
        verify(statusSyncMessageService, never()).sendRefundStatusSyncMessage(anyString(), any(Integer.class));
    }

    @Test
    void legacyMessageIdentityIsStableAndIncludesAttempt() {
        String expected = UUID.nameUUIDFromBytes(
                "REFUND_STATUS_SYNC_REQUESTED:REFUND-1:4".getBytes(StandardCharsets.UTF_8)
        ).toString();
        when(consumeLogService.tryStart(
                expected, "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:4"))
                .thenReturn(MessageConsumeClaim.consumed());

        consumer.handleRefundStatusSync(message(null, "REFUND-1", 4));

        verify(consumeLogService).tryStart(
                expected, "refund-status-sync",
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED, "REFUND-1:4");
    }

    @Test
    void rejectedReleaseMessagesReturnThroughTheDelayQueue() {
        RefundStatusSyncRabbitConfig config = new RefundStatusSyncRabbitConfig();
        Queue releaseQueue = config.refundStatusSyncReleaseQueue();

        assertThat(releaseQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY);
    }

    @Test
    void emptyMessageDoesNotTouchInboxOrChannel() {
        consumer.handleRefundStatusSync(new RefundStatusSyncMessage());

        verifyNoInteractions(refundApplicationService, consumeLogService, statusSyncMessageService, transactionTemplate);
    }

    private RefundStatusSyncMessage message(String eventId, String refundNo, int attempt) {
        RefundStatusSyncMessage message = new RefundStatusSyncMessage();
        message.setEventId(eventId);
        message.setRefundNo(refundNo);
        message.setAttempt(attempt);
        return message;
    }

    private RefundInfo refund(String refundNo, RefundStatus status) {
        RefundInfo refund = new RefundInfo();
        refund.setRefundNo(refundNo);
        refund.setRefundStatus(status.getType());
        return refund;
    }
}

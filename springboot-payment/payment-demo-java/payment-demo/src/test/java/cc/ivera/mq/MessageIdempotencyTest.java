package cc.ivera.mq;

import cc.ivera.entity.MessageConsumeLog;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.MessageConsumeLogMapper;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.impl.MessageConsumeLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Update;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageIdempotencyTest {

    private MessageConsumeLogMapper mapper;
    private MessageConsumeLogServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(MessageConsumeLogMapper.class);
        service = new MessageConsumeLogServiceImpl(mapper, "test-worker", 30);
    }

    @Test
    void firstDeliveryClaimsTheInboxLease() {
        when(mapper.insert(any(MessageConsumeLog.class), anyLong())).thenReturn(1);

        MessageConsumeClaim claim = service.tryStart(
                "event-1",
                "refund-submit",
                "REFUND_SUBMIT_REQUESTED",
                "REFUND-1"
        );

        assertThat(claim.getStatus()).isEqualTo(MessageConsumeClaim.Status.CLAIMED);
        assertThat(claim.getLeaseToken()).startsWith("test-worker:");
    }

    @Test
    void duplicateActiveDeliveryIsBusyRatherThanAcknowledgedAsConsumed() {
        when(mapper.insert(any(MessageConsumeLog.class), anyLong()))
                .thenThrow(new DuplicateKeyException("duplicate inbox event"));
        when(mapper.reclaimExpiredOrFailed(
                org.mockito.ArgumentMatchers.eq("event-1"),
                org.mockito.ArgumentMatchers.eq("refund-submit"),
                anyString(),
                anyLong()
        )).thenReturn(0);
        when(mapper.selectByEventAndConsumer("event-1", "refund-submit"))
                .thenReturn(log("REFUND_SUBMIT_REQUESTED", "REFUND-1"));

        MessageConsumeClaim claim = service.tryStart(
                "event-1",
                "refund-submit",
                "REFUND_SUBMIT_REQUESTED",
                "REFUND-1"
        );

        assertThat(claim.getStatus()).isEqualTo(MessageConsumeClaim.Status.BUSY);
    }

    @Test
    void completedDeliveryIsReportedSeparatelyFromABusyLease() {
        when(mapper.insert(any(MessageConsumeLog.class), anyLong()))
                .thenThrow(new DuplicateKeyException("duplicate inbox event"));
        MessageConsumeLog existing = log("REFUND_SUBMIT_REQUESTED", "REFUND-1");
        existing.setStatus("CONSUMED");
        when(mapper.selectByEventAndConsumer("event-1", "refund-submit")).thenReturn(existing);

        MessageConsumeClaim claim = service.tryStart(
                "event-1",
                "refund-submit",
                "REFUND_SUBMIT_REQUESTED",
                "REFUND-1"
        );

        assertThat(claim.getStatus()).isEqualTo(MessageConsumeClaim.Status.CONSUMED);
    }

    @Test
    void duplicateEventIdentityWithDifferentBusinessDataIsAConflict() {
        when(mapper.insert(any(MessageConsumeLog.class), anyLong()))
                .thenThrow(new DuplicateKeyException("duplicate inbox event"));
        when(mapper.reclaimExpiredOrFailed(
                org.mockito.ArgumentMatchers.eq("event-1"),
                org.mockito.ArgumentMatchers.eq("refund-submit"),
                anyString(),
                anyLong()
        )).thenReturn(0);
        when(mapper.selectByEventAndConsumer("event-1", "refund-submit"))
                .thenReturn(log("REFUND_SUBMIT_REQUESTED", "REFUND-OTHER"));

        assertThatThrownBy(() -> service.tryStart(
                "event-1",
                "refund-submit",
                "REFUND_SUBMIT_REQUESTED",
                "REFUND-1"
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("消费事件参数冲突");
    }

    @Test
    void inboxLeaseSqlUsesARealComparisonOperator() throws Exception {
        Update update = MessageConsumeLogMapper.class.getMethod(
                "reclaimExpiredOrFailed",
                String.class,
                String.class,
                String.class,
                long.class
        ).getAnnotation(Update.class);

        assertThat(String.join(" ", update.value()))
                .contains("<=")
                .doesNotContain("&lt;");
    }

    @Test
    void oversizedInboxIdentityIsRejectedBeforeSqlExecution() {
        assertThatThrownBy(() -> service.tryStart(
                repeat('e', 37),
                "refund-submit",
                "REFUND_SUBMIT_REQUESTED",
                "REFUND-1"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("消费事件ID长度");

        verifyNoInteractions(mapper);
    }

    private MessageConsumeLog log(String eventType, String businessKey) {
        MessageConsumeLog log = new MessageConsumeLog();
        log.setEventId("event-1");
        log.setConsumerName("refund-submit");
        log.setEventType(eventType);
        log.setBusinessKey(businessKey);
        log.setStatus("PROCESSING");
        return log;
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}

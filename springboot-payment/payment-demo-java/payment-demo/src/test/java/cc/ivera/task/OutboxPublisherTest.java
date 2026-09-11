package cc.ivera.task;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.config.RefundSubmitRabbitConfig;
import cc.ivera.entity.MessageOutbox;
import cc.ivera.mapper.MessageOutboxMapper;
import cc.ivera.mq.OrderCloseMessage;
import cc.ivera.mq.RefundStatusSyncMessage;
import cc.ivera.mq.RefundSubmitMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private MessageOutboxMapper mapper;
    private RabbitTemplate rabbitTemplate;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        mapper = mock(MessageOutboxMapper.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new OutboxPublisher(mapper, rabbitTemplate);
    }

    @Test
    void confirmedOrderCloseEventIsMarkedSent() {
        MessageOutbox event = orderCloseEvent(0);
        when(mapper.selectPublishable(anyInt()))
                .thenReturn(Collections.singletonList(event));
        when(mapper.claimForPublish(eq(1L), anyString(), anyLong()))
                .thenReturn(1);
        when(mapper.markSent(eq(1L), anyString(), any(Date.class))).thenReturn(1);
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE),
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY),
                any(OrderCloseMessage.class),
                any(CorrelationData.class)
        );

        publisher.publishPending();

        ArgumentCaptor<OrderCloseMessage> message = ArgumentCaptor.forClass(OrderCloseMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE),
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY),
                message.capture(),
                any(CorrelationData.class)
        );
        assertThat(message.getValue().getEventId()).isEqualTo("event-1");
        assertThat(message.getValue().getOrderNo()).isEqualTo("ORDER-1");
        verify(mapper).markSent(eq(1L), anyString(), any(Date.class));
        verify(mapper, never()).markFailed(eq(1L), anyString(), anyString(), any());
    }

    @Test
    void confirmedRefundSubmitEventUsesTheSubmitRouteAndCarriesTheOutboxIdentity() {
        MessageOutbox event = event(
                "event-refund-submit",
                "REFUND_SUBMIT_REQUESTED:REFUND-1",
                "REFUND_SUBMIT_REQUESTED",
                "{\"refundNo\":\"REFUND-1\"}"
        );
        stubConfirmedPublish(event);

        publisher.publishPending();

        ArgumentCaptor<RefundSubmitMessage> message = ArgumentCaptor.forClass(RefundSubmitMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RefundSubmitRabbitConfig.REFUND_SUBMIT_EVENT_EXCHANGE),
                eq(RefundSubmitRabbitConfig.REFUND_SUBMIT_ROUTING_KEY),
                message.capture(),
                any(CorrelationData.class)
        );
        assertThat(message.getValue().getEventId()).isEqualTo("event-refund-submit");
        assertThat(message.getValue().getRefundNo()).isEqualTo("REFUND-1");
    }

    @Test
    void confirmedRefundStatusSyncEventCarriesItsAttempt() {
        MessageOutbox event = event(
                "event-refund-status-3",
                "REFUND_STATUS_SYNC_REQUESTED:REFUND-1:3",
                "REFUND_STATUS_SYNC_REQUESTED",
                "{\"refundNo\":\"REFUND-1\",\"attempt\":3}"
        );
        stubConfirmedPublish(event);

        publisher.publishPending();

        ArgumentCaptor<RefundStatusSyncMessage> message = ArgumentCaptor.forClass(RefundStatusSyncMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE),
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY),
                message.capture(),
                any(CorrelationData.class)
        );
        assertThat(message.getValue().getEventId()).isEqualTo("event-refund-status-3");
        assertThat(message.getValue().getRefundNo()).isEqualTo("REFUND-1");
        assertThat(message.getValue().getAttempt()).isEqualTo(3);
    }

    @Test
    void nackSchedulesABoundedRetryAndDoesNotMarkSent() {
        MessageOutbox event = orderCloseEvent(0);
        when(mapper.selectPublishable(anyInt()))
                .thenReturn(Collections.singletonList(event));
        when(mapper.claimForPublish(eq(1L), anyString(), anyLong()))
                .thenReturn(1);
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(false, "broker nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(OrderCloseMessage.class), any(CorrelationData.class));

        long before = System.currentTimeMillis();
        publisher.publishPending();

        ArgumentCaptor<Date> nextRetry = ArgumentCaptor.forClass(Date.class);
        verify(mapper).markFailed(eq(1L), anyString(),
                org.mockito.ArgumentMatchers.contains("broker nack"), nextRetry.capture());
        assertThat(nextRetry.getValue().getTime()).isBetween(before + 500L, before + 60_000L);
        verify(mapper, never()).markSent(eq(1L), anyString(), any(Date.class));
    }

    @Test
    void exhaustedEventStaysFailedUntilAnAdminRetriesIt() {
        MessageOutbox event = orderCloseEvent(4);
        when(mapper.selectPublishable(anyInt()))
                .thenReturn(Collections.singletonList(event));
        when(mapper.claimForPublish(eq(1L), anyString(), anyLong()))
                .thenReturn(1);
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(false, "still down"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(OrderCloseMessage.class), any(CorrelationData.class));

        publisher.publishPending();

        verify(mapper).markFailed(eq(1L), anyString(), anyString(), isNull());
    }

    @Test
    void eventClaimedByAnotherWorkerIsNotPublished() {
        MessageOutbox event = orderCloseEvent(0);
        when(mapper.selectPublishable(anyInt()))
                .thenReturn(Collections.singletonList(event));
        when(mapper.claimForPublish(eq(1L), anyString(), anyLong()))
                .thenReturn(0);

        publisher.publishPending();

        verify(rabbitTemplate, never()).convertAndSend(
                anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }

    private MessageOutbox orderCloseEvent(int retryCount) {
        MessageOutbox event = event(
                "event-1",
                "ORDER_CLOSE_SCHEDULED:ORDER-1",
                "ORDER_CLOSE_SCHEDULED",
                "{\"orderNo\":\"ORDER-1\",\"paymentType\":\"微信\"}"
        );
        event.setRetryCount(retryCount);
        return event;
    }

    private MessageOutbox event(String eventId, String eventKey, String eventType, String payload) {
        MessageOutbox event = new MessageOutbox();
        event.setId(1L);
        event.setEventId(eventId);
        event.setEventKey(eventKey);
        event.setAggregateType(eventType.startsWith("ORDER") ? "ORDER" : "REFUND");
        event.setAggregateId(eventType.startsWith("ORDER") ? "ORDER-1" : "REFUND-1");
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus("NEW");
        event.setRetryCount(0);
        return event;
    }

    private void stubConfirmedPublish(MessageOutbox event) {
        when(mapper.selectPublishable(anyInt())).thenReturn(Collections.singletonList(event));
        when(mapper.claimForPublish(eq(1L), anyString(), anyLong())).thenReturn(1);
        when(mapper.markSent(eq(1L), anyString(), any(Date.class))).thenReturn(1);
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }
}

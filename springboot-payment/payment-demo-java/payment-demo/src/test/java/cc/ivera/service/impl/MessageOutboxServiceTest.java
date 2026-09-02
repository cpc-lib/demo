package cc.ivera.service.impl;

import cc.ivera.entity.MessageOutbox;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.MessageOutboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageOutboxServiceTest {

    private MessageOutboxMapper mapper;
    private MessageOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(MessageOutboxMapper.class);
        service = new MessageOutboxServiceImpl(mapper);
    }

    @Test
    void insertOnceCreatesOneNewEventWithAStableEventId() {
        when(mapper.insert(any(MessageOutbox.class))).thenReturn(1);

        MessageOutbox result = service.insertOnce(
                "ORDER_CLOSE_SCHEDULED:ORDER-1",
                "ORDER",
                "ORDER-1",
                "ORDER_CLOSE_SCHEDULED",
                "{\"orderNo\":\"ORDER-1\"}"
        );

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(mapper).insert(captor.capture());
        MessageOutbox inserted = captor.getValue();
        assertThat(inserted.getEventId()).isNotBlank();
        assertThat(inserted.getEventKey()).isEqualTo("ORDER_CLOSE_SCHEDULED:ORDER-1");
        assertThat(inserted.getStatus()).isEqualTo("NEW");
        assertThat(inserted.getRetryCount()).isZero();
        assertThat(result).isSameAs(inserted);
    }

    @Test
    void duplicateEventKeyWithEqualContentReturnsTheExistingEvent() {
        MessageOutbox existing = event("event-1", "{\"orderNo\":\"ORDER-1\"}");
        when(mapper.insert(any(MessageOutbox.class)))
                .thenThrow(new DuplicateKeyException("duplicate event key"));
        when(mapper.selectByEventKey("ORDER_CLOSE_SCHEDULED:ORDER-1")).thenReturn(existing);

        MessageOutbox result = service.insertOnce(
                "ORDER_CLOSE_SCHEDULED:ORDER-1",
                "ORDER",
                "ORDER-1",
                "ORDER_CLOSE_SCHEDULED",
                "{\"orderNo\":\"ORDER-1\"}"
        );

        assertThat(result).isSameAs(existing);
    }

    @Test
    void duplicateEventKeyWithDifferentContentIsAConflict() {
        MessageOutbox existing = event("event-1", "{\"orderNo\":\"ORDER-OTHER\"}");
        when(mapper.insert(any(MessageOutbox.class)))
                .thenThrow(new DuplicateKeyException("duplicate event key"));
        when(mapper.selectByEventKey("ORDER_CLOSE_SCHEDULED:ORDER-1")).thenReturn(existing);

        assertThatThrownBy(() -> service.insertOnce(
                "ORDER_CLOSE_SCHEDULED:ORDER-1",
                "ORDER",
                "ORDER-1",
                "ORDER_CLOSE_SCHEDULED",
                "{\"orderNo\":\"ORDER-1\"}"
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("业务键参数冲突");
    }

    @Test
    void adminRetryResetsOnlyAnExistingFailedEvent() {
        MessageOutbox failed = event("event-1", "{\"orderNo\":\"ORDER-1\"}");
        failed.setStatus("FAILED");
        failed.setRetryCount(5);
        when(mapper.selectByEventId("event-1")).thenReturn(failed);
        when(mapper.resetFailed("event-1")).thenReturn(1);

        service.retryFailed("event-1");

        verify(mapper).resetFailed("event-1");
    }

    @Test
    void adminRetryRejectsMissingOrNonFailedEvents() {
        when(mapper.selectByEventId("missing")).thenReturn(null);
        MessageOutbox sent = event("event-sent", "{\"orderNo\":\"ORDER-1\"}");
        sent.setStatus("SENT");
        when(mapper.selectByEventId("event-sent")).thenReturn(sent);

        assertThatThrownBy(() -> service.retryFailed("missing"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.retryFailed("event-sent"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("只有失败消息");
    }

    @Test
    void oversizedEventKeyIsRejectedBeforeWriting() {
        assertThatThrownBy(() -> service.insertOnce(
                repeat('x', 129),
                "ORDER",
                "ORDER-1",
                "ORDER_CLOSE_SCHEDULED",
                "{}"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("消息业务键长度");

        verifyNoInteractions(mapper);
    }

    private MessageOutbox event(String eventId, String payload) {
        MessageOutbox event = new MessageOutbox();
        event.setEventId(eventId);
        event.setEventKey("ORDER_CLOSE_SCHEDULED:ORDER-1");
        event.setAggregateType("ORDER");
        event.setAggregateId("ORDER-1");
        event.setEventType("ORDER_CLOSE_SCHEDULED");
        event.setPayload(payload);
        event.setStatus("NEW");
        event.setRetryCount(0);
        return event;
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}

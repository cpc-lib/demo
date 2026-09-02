package cc.ivera.service.impl;

import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderCloseOutboxServiceTest {

    @Test
    void orderCloseScheduleIsPersistedAsAnIdempotentOutboxEvent() {
        MessageOutboxService outboxService = mock(MessageOutboxService.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(outboxService);

        service.sendCloseOrderMessage("ORD-CLOSE", "微信");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).insertOnce(
                eq("ORDER_CLOSE_SCHEDULED:ORD-CLOSE"),
                eq("ORDER"),
                eq("ORD-CLOSE"),
                eq(OutboxEventTypes.ORDER_CLOSE_SCHEDULED),
                payload.capture()
        );
        Map<String, Object> body = JsonUtils.toObjectMap(payload.getValue());
        assertThat(body).containsEntry("orderNo", "ORD-CLOSE")
                .containsEntry("paymentType", "微信");
    }
}

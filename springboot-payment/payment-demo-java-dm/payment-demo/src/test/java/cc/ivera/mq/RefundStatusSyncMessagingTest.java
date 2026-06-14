package cc.ivera.mq;

import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.impl.RefundStatusSyncMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RefundStatusSyncMessagingTest {

    @Test
    void sends_refund_status_sync_message_to_delay_exchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RefundStatusSyncMessageServiceImpl service = new RefundStatusSyncMessageServiceImpl(rabbitTemplate);

        service.sendRefundStatusSyncMessage("REFUND-1");

        ArgumentCaptor<RefundStatusSyncMessage> captor = ArgumentCaptor.forClass(RefundStatusSyncMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE),
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY),
                captor.capture());
        assertThat(captor.getValue().getRefundNo()).isEqualTo("REFUND-1");
    }

    @Test
    void rejects_blank_refund_no_before_sending_message() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RefundStatusSyncMessageServiceImpl service = new RefundStatusSyncMessageServiceImpl(rabbitTemplate);

        assertThatThrownBy(() -> service.sendRefundStatusSyncMessage(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refundNo must not be blank");

        verify(rabbitTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(RefundStatusSyncMessage.class));
    }

    @Test
    void consumer_uses_existing_refund_status_query_service() {
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);
        RefundStatusSyncConsumer consumer = new RefundStatusSyncConsumer(refundApplicationService);
        RefundStatusSyncMessage message = new RefundStatusSyncMessage();
        message.setRefundNo("REFUND-1");

        consumer.handleRefundStatusSync(message);

        verify(refundApplicationService).queryRefundStatus("REFUND-1");
    }

    @Test
    void consumer_ignores_empty_message() {
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);
        RefundStatusSyncConsumer consumer = new RefundStatusSyncConsumer(refundApplicationService);
        RefundStatusSyncMessage message = new RefundStatusSyncMessage();

        consumer.handleRefundStatusSync(message);

        verify(refundApplicationService, never()).queryRefundStatus(org.mockito.ArgumentMatchers.anyString());
    }
}

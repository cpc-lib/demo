package cc.ivera.mq;

import cc.ivera.entity.OrderInfo;
import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.service.AliPayService;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderCloseConsumerInventoryTest {

    private OrderInfoService orderInfoService;
    private WxPayOrderFacade wxPayOrderFacade;
    private AliPayService aliPayService;
    private MessageConsumeLogService consumeLogService;
    private OrderCloseConsumer consumer;

    @BeforeEach
    void setUp() {
        orderInfoService = mock(OrderInfoService.class);
        wxPayOrderFacade = mock(WxPayOrderFacade.class);
        aliPayService = mock(AliPayService.class);
        consumeLogService = mock(MessageConsumeLogService.class);
        consumer = new OrderCloseConsumer(
                orderInfoService,
                wxPayOrderFacade,
                aliPayService,
                consumeLogService
        );
    }

    @Test
    void claimedAlipayMessageRunsBusinessAndCompletesTheInboxLease() {
        when(consumeLogService.tryStart(
                "event-1",
                "order-close",
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                "ORDER-ALI-DELAY"
        )).thenReturn(MessageConsumeClaim.claimed("lease-1"));
        when(orderInfoService.getOrderByOrderNo("ORDER-ALI-DELAY"))
                .thenReturn(unpaidOrder("ORDER-ALI-DELAY", PayType.ALIPAY.getType()));

        consumer.handleOrderClose(message("event-1", "ORDER-ALI-DELAY"));

        verify(aliPayService).checkOrderStatusAndCloseIfUnpaid("ORDER-ALI-DELAY");
        verify(consumeLogService).complete("event-1", "order-close", "lease-1");
    }

    @Test
    void consumedDuplicateAcknowledgesWithoutRepeatingBusiness() {
        when(consumeLogService.tryStart(
                "event-1",
                "order-close",
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                "ORDER-1"
        )).thenReturn(MessageConsumeClaim.consumed());

        consumer.handleOrderClose(message("event-1", "ORDER-1"));

        verifyNoInteractions(orderInfoService, wxPayOrderFacade, aliPayService);
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void busyDuplicateIsRejectedIntoTheDelayedRetryPath() {
        when(consumeLogService.tryStart(
                "event-1",
                "order-close",
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                "ORDER-1"
        )).thenReturn(MessageConsumeClaim.busy());

        assertThatThrownBy(() -> consumer.handleOrderClose(message("event-1", "ORDER-1")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("正在处理");

        verifyNoInteractions(orderInfoService, wxPayOrderFacade, aliPayService);
    }

    @Test
    void legacyMessageWithoutEventIdGetsAStableUuidIdentity() {
        when(consumeLogService.tryStart(
                anyString(),
                eq("order-close"),
                eq(OutboxEventTypes.ORDER_CLOSE_SCHEDULED),
                eq("ORDER-LEGACY")
        )).thenReturn(MessageConsumeClaim.claimed("lease-legacy"));
        when(orderInfoService.getOrderByOrderNo("ORDER-LEGACY"))
                .thenReturn(unpaidOrder("ORDER-LEGACY", PayType.WXPAY.getType()));

        consumer.handleOrderClose(message(null, "ORDER-LEGACY"));

        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        verify(consumeLogService).tryStart(
                eventId.capture(),
                eq("order-close"),
                eq(OutboxEventTypes.ORDER_CLOSE_SCHEDULED),
                eq("ORDER-LEGACY")
        );
        assertThat(eventId.getValue()).hasSize(36);
        verify(consumeLogService).complete(eventId.getValue(), "order-close", "lease-legacy");
    }

    @Test
    void channelFailureMarksTheLeaseFailedAndRejectsForDelayedRetry() {
        when(consumeLogService.tryStart(
                "event-1",
                "order-close",
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                "ORDER-WX"
        )).thenReturn(MessageConsumeClaim.claimed("lease-1"));
        when(orderInfoService.getOrderByOrderNo("ORDER-WX"))
                .thenReturn(unpaidOrder("ORDER-WX", PayType.WXPAY.getType()));
        doThrow(new BizException("channel unavailable"))
                .when(wxPayOrderFacade).checkOrderStatus("ORDER-WX");

        assertThatThrownBy(() -> consumer.handleOrderClose(message("event-1", "ORDER-WX")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("关单消息处理失败");

        verify(consumeLogService).fail(
                eq("event-1"),
                eq("order-close"),
                eq("lease-1"),
                org.mockito.ArgumentMatchers.contains("channel unavailable"),
                isNull()
        );
        verify(consumeLogService, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void rejectedReleaseMessagesReturnThroughTheDelayQueueInsteadOfHotLooping() {
        Queue releaseQueue = new OrderCloseRabbitConfig().orderCloseReleaseQueue();

        assertThat(releaseQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY);
    }

    private OrderCloseMessage message(String eventId, String orderNo) {
        OrderCloseMessage message = new OrderCloseMessage();
        message.setEventId(eventId);
        message.setOrderNo(orderNo);
        return message;
    }

    private OrderInfo unpaidOrder(String orderNo, String paymentType) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setPaymentType(paymentType);
        return order;
    }
}

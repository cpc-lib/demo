package cc.ivera.mq;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.service.AliPayService;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class OrderCloseConsumer {

    private static final String CONSUMER_NAME = "order-close";

    private final OrderInfoService orderInfoService;

    private final WxPayOrderFacade wxPayOrderFacade;

    private final AliPayService aliPayService;

    private final MessageConsumeLogService consumeLogService;

    public OrderCloseConsumer(
        OrderInfoService orderInfoService,
        WxPayOrderFacade wxPayOrderFacade,
        AliPayService aliPayService,
        MessageConsumeLogService consumeLogService
    ) {
        this.orderInfoService = orderInfoService;
        this.wxPayOrderFacade = wxPayOrderFacade;
        this.aliPayService = aliPayService;
        this.consumeLogService = consumeLogService;
    }

    @RabbitListener(queues = OrderCloseRabbitConfig.ORDER_CLOSE_RELEASE_QUEUE)
    public void handleOrderClose(OrderCloseMessage message) {
        if (message == null || !StringUtils.hasText(message.getOrderNo())) {
            log.warn("收到空的延迟关单消息，忽略处理");
            return;
        }

        String orderNo = message.getOrderNo();
        String eventId = resolveEventId(message, orderNo);
        MessageConsumeClaim claim;
        try {
            claim = consumeLogService.tryStart(
                    eventId,
                    CONSUMER_NAME,
                    OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                    orderNo
            );
        } catch (RuntimeException exception) {
            throw retryLater("关单消息认领失败", exception);
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.CONSUMED) {
            log.info("延迟关单消息已处理，忽略重复投递，eventId={}, orderNo={}", eventId, orderNo);
            return;
        }
        if (claim.getStatus() == MessageConsumeClaim.Status.BUSY) {
            throw retryLater("延迟关单消息正在处理，请稍后重试", null);
        }

        try {
            processOrderClose(orderNo);
            consumeLogService.complete(eventId, CONSUMER_NAME, claim.getLeaseToken());
        } catch (RuntimeException exception) {
            consumeLogService.fail(
                    eventId,
                    CONSUMER_NAME,
                    claim.getLeaseToken(),
                    exception.getMessage(),
                    null
            );
            throw retryLater("关单消息处理失败", exception);
        }
    }

    private void processOrderClose(String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            log.warn("延迟关单时订单不存在，orderNo={}", orderNo);
            return;
        }

        if (!OrderStatus.NOTPAY.getType().equals(orderInfo.getOrderStatus())) {
            log.info("订单当前状态无需关单，orderNo={}, status={}", orderNo, orderInfo.getOrderStatus());
            return;
        }

        String paymentType = orderInfo.getPaymentType();
        log.info("开始处理延迟关单，orderNo={}, paymentType={}", orderNo, paymentType);

        if (PayType.WXPAY.getType().equals(paymentType)) {
            wxPayOrderFacade.checkOrderStatus(orderNo);
            return;
        }
        if (PayType.ALIPAY.getType().equals(paymentType)) {
            aliPayService.checkOrderStatusAndCloseIfUnpaid(orderNo);
            return;
        }

        log.warn("未知支付类型，无法处理延迟关单，orderNo={}, paymentType={}", orderNo, paymentType);
    }

    private String resolveEventId(OrderCloseMessage message, String orderNo) {
        if (StringUtils.hasText(message.getEventId()) && message.getEventId().length() <= 36) {
            return message.getEventId();
        }
        String identity = OutboxEventTypes.ORDER_CLOSE_SCHEDULED + ":" + orderNo;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private AmqpRejectAndDontRequeueException retryLater(String message, RuntimeException cause) {
        if (cause == null) {
            return new AmqpRejectAndDontRequeueException(message);
        }
        return new AmqpRejectAndDontRequeueException(message, cause);
    }
}

package cc.ivera.mq;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class OrderCloseConsumer {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @Resource
    private AliPayService aliPayService;

    @RabbitListener(queues = OrderCloseRabbitConfig.ORDER_CLOSE_RELEASE_QUEUE)
    public void handleOrderClose(OrderCloseMessage message) throws Exception {
        if (message == null || message.getOrderNo() == null) {
            log.warn("收到空的延迟关单消息，忽略处理");
            return;
        }

        String orderNo = message.getOrderNo();
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
            wxPayService.checkOrderStatus(orderNo);
            return;
        }
        if (PayType.ALIPAY.getType().equals(paymentType)) {
            aliPayService.checkOrderStatus(orderNo);
            return;
        }

        log.warn("未知支付类型，无法处理延迟关单，orderNo={}, paymentType={}", orderNo, paymentType);
    }
}

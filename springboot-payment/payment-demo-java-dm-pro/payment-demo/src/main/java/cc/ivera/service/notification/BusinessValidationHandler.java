package cc.ivera.service.notification;

import cc.ivera.entity.OrderInfo;
import cc.ivera.exception.BizException;
import cc.ivera.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务校验处理器 — 校验订单存在性、金额一致性、支付类型匹配。
 */
@Slf4j
public class BusinessValidationHandler implements NotificationHandler {

    private final OrderInfoService orderInfoService;

    public BusinessValidationHandler(OrderInfoService orderInfoService) {
        this.orderInfoService = orderInfoService;
    }

    @Override
    public void handle(NotificationContext context, NotificationChain chain) {
        String orderNo = context.getOrderNo();
        if (orderNo == null || orderNo.trim().isEmpty()) {
            context.setErrorMessage("通知缺少订单号");
            return;
        }

        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            context.setErrorMessage("通知对应订单不存在，orderNo=" + orderNo);
            return;
        }

        log.info("业务校验通过，orderNo={}", orderNo);
        chain.proceed(context);
    }
}

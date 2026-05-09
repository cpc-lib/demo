package cc.ivera.service.impl.wxpay;

import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.exception.BizException;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
public class WxPayOrderNotifyService {

    private final OrderInfoService orderInfoService;

    private final PaymentInfoService paymentInfoService;

    public WxPayOrderNotifyService(OrderInfoService orderInfoService,
                                   PaymentInfoService paymentInfoService) {
        this.orderInfoService = orderInfoService;
        this.paymentInfoService = paymentInfoService;
    }

    /**
     * 由持锁方调用，确保支付通知事务在订单锁内执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void processNativeNotifyInTransaction(String orderNo, String plainText) {
        markOrderPaidAndWriteLog(orderNo, () -> paymentInfoService.createPaymentInfo(plainText), "微信支付通知");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processNativeV2NotifyInTransaction(String orderNo,
                                                   Map<String, String> notifyMap,
                                                   String content) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
        if (orderInfo == null) {
            throw new BizException("订单不存在");
        }

        Long totalFee = parseTotalFee(notifyMap.get("total_fee"));
        if (totalFee == null || !totalFee.equals(orderInfo.getTotalFee())) {
            throw new BizException("金额校验失败");
        }

        markOrderPaidAndWriteLog(orderNo,
                () -> paymentInfoService.createPaymentInfoForWxPayV2(notifyMap, content),
                "微信支付v2通知");
    }

    private void markOrderPaidAndWriteLog(String orderNo, Runnable writePaymentLog, String source) {
        boolean updated = orderInfoService.updateStatusByOrderNoIfStatus(
                orderNo,
                OrderStatus.NOTPAY,
                OrderStatus.SUCCESS);
        if (!updated) {
            log.info("{}重复或订单状态已变化，忽略处理 ===> {}", source, orderNo);
            return;
        }
        writePaymentLog.run();
    }

    private Long parseTotalFee(String totalFee) {
        try {
            return Long.parseLong(totalFee);
        } catch (NumberFormatException e) {
            log.error("微信支付v2通知金额格式错误 ===> {}", totalFee, e);
            return null;
        }
    }
}

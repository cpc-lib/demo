package cc.ivera.service.wxpay;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;

import java.util.Map;

public interface WxPayOrderFacade {

    Map<String, Object> nativePay(Long productId, Long paymentAppId, String idempotencyKey);

    Map<String, Object> nativePayOrder(String orderNo);

    Map<String, Object> createPayment(
            PaymentOrder paymentOrder,
            OrderInfo orderInfo,
            String remoteAddr
    );

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    Map<String, Object> queryPaymentStatus(String orderNo);

    void checkOrderStatus(String orderNo);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr,
                                    Long paymentAppId, String idempotencyKey);

    Map<String, Object> nativePayV2Order(String orderNo, String remoteAddr);

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}

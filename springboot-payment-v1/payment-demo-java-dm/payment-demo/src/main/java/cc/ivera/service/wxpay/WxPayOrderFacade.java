package cc.ivera.service.wxpay;

import cc.ivera.entity.OrderInfo;

import java.util.Map;

public interface WxPayOrderFacade {

    Map<String, Object> nativePay(Long productId);

    Map<String, Object> nativePay(Long productId, Long paymentAppId);

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    Map<String, Object> queryPaymentStatus(String orderNo);

    void checkOrderStatus(String orderNo);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr, Long paymentAppId);

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}

package cc.ivera.service.wxpay;

import cc.ivera.entity.OrderInfo;

import java.util.Map;

public interface WxPayOrderFacade {

    Map<String, Object> nativePay(Long productId);

    void processOrder(Map<String, Object> bodyMap);

    void cancelOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    Map<String, Object> nativePayV2(Long productId, String remoteAddr);

    Map<String, Object> jsapiPay(OrderInfo orderInfo, String openid);
}

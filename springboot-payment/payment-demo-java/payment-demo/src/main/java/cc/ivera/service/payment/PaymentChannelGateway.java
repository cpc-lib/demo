package cc.ivera.service.payment;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;

import java.util.Map;

public interface PaymentChannelGateway {

    Map<String, Object> create(PaymentOrder paymentOrder, OrderInfo orderInfo, String remoteAddr);
}

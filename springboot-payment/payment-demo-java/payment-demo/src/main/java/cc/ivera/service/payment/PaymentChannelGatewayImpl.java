package cc.ivera.service.payment;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.service.AliPayService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentChannelGatewayImpl implements PaymentChannelGateway {

    private final WxPayOrderFacade wxPayOrderFacade;

    private final AliPayService aliPayService;

    public PaymentChannelGatewayImpl(
            WxPayOrderFacade wxPayOrderFacade,
            AliPayService aliPayService
    ) {
        this.wxPayOrderFacade = wxPayOrderFacade;
        this.aliPayService = aliPayService;
    }

    @Override
    public Map<String, Object> create(
            PaymentOrder paymentOrder,
            OrderInfo orderInfo,
            String remoteAddr
    ) {
        if (PaymentConfigLoader.CHANNEL_WXPAY.equals(paymentOrder.getChannel())) {
            return wxPayOrderFacade.createPayment(paymentOrder, orderInfo, remoteAddr);
        }
        if (PaymentConfigLoader.CHANNEL_ALIPAY.equals(paymentOrder.getChannel())) {
            Map<String, Object> params = new HashMap<>();
            params.put("formStr", aliPayService.createPayment(paymentOrder, orderInfo));
            params.put("orderNo", paymentOrder.getOrderNo());
            params.put("paymentNo", paymentOrder.getPaymentNo());
            return params;
        }
        throw new PaymentChannelRejectedException("不支持的支付渠道：" + paymentOrder.getChannel());
    }
}

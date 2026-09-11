package cc.ivera.vo;

import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentDisposition;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.util.JsonUtils;
import lombok.Data;

import java.util.Date;

@Data
public class PaymentOrderView {

    private String paymentNo;
    private String orderNo;
    private Long paymentAppId;
    private String channel;
    private String protocol;
    private Long requestAmount;
    private Long paidAmount;
    private String currency;
    private PaymentOrderStatus status;
    private PaymentDisposition disposition;
    private String requestId;
    private Object paymentParams;
    private Date expiresAt;
    private Date paidAt;

    public static PaymentOrderView from(PaymentOrder paymentOrder) {
        PaymentOrderView view = new PaymentOrderView();
        view.setPaymentNo(paymentOrder.getPaymentNo());
        view.setOrderNo(paymentOrder.getOrderNo());
        view.setPaymentAppId(paymentOrder.getPaymentAppId());
        view.setChannel(paymentOrder.getChannel());
        view.setProtocol(paymentOrder.getProtocol());
        view.setRequestAmount(paymentOrder.getRequestAmount());
        view.setPaidAmount(paymentOrder.getPaidAmount());
        view.setCurrency(paymentOrder.getCurrency());
        view.setStatus(paymentOrder.getStatus());
        view.setDisposition(paymentOrder.getDisposition());
        view.setRequestId(paymentOrder.getRequestId());
        if (paymentOrder.getPaymentParams() != null
                && !paymentOrder.getPaymentParams().trim().isEmpty()) {
            view.setPaymentParams(JsonUtils.toObjectMap(paymentOrder.getPaymentParams()));
        }
        view.setExpiresAt(paymentOrder.getExpiresAt());
        view.setPaidAt(paymentOrder.getPaidAt());
        return view;
    }
}

package cc.ivera.vo;

import cc.ivera.entity.OrderInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutResult {

    private String orderNo;

    private Integer totalFee;

    private Long paymentAppId;

    private String paymentChannelCode;

    private String orderStatus;

    public static CheckoutResult from(OrderInfo order) {
        return new CheckoutResult(
                order.getOrderNo(),
                order.getTotalFee(),
                order.getPaymentAppId(),
                order.getPaymentChannelCode(),
                order.getOrderStatus()
        );
    }
}

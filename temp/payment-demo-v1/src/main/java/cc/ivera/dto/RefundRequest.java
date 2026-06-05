package cc.ivera.dto;

import lombok.Data;

@Data
public class RefundRequest {

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 本次退款金额（分）
     * 为空时，默认退剩余可退金额
     */
    private Integer refundAmount;

    /**
     * 退款原因
     */
    private String reason;
}

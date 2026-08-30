package cc.ivera.service.impl.reconciliation;

import lombok.Data;

import java.util.Date;

@Data
public class ChannelBillRecord {

    /** PAYMENT（进账）或 REFUND（退款） */
    private String businessType;

    private String orderNo;

    private String transactionId;

    private Integer amount;

    private String status;

    private String tradeType;

    private Date tradeTime;

    private Integer refundAmount;

    private String refundNo;

    private String refundId;
}

package cc.ivera.service.impl.reconciliation;

import lombok.Data;

import java.util.Date;

@Data
public class ChannelBillRecord {

    private String orderNo;

    private String transactionId;

    private Integer amount;

    private String status;

    private String tradeType;

    private Date tradeTime;

    private Integer refundAmount;

    private String refundId;
}

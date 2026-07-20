package cc.ivera.vo.reconciliation;

import lombok.Data;

import java.util.Date;

@Data
public class ReconciliationBatchVO {

    private Long id;

    private String batchNo;

    private String channelCode;

    private Long paymentAppId;

    private String billDate;

    private String status;

    private Integer channelTotalCount;

    private Integer channelTotalAmount;

    private Integer localTotalCount;

    private Integer localTotalAmount;

    private Integer matchedCount;

    private Integer matchedAmount;

    private Integer discrepancyCount;

    private Integer overpaymentCount;

    private Integer underpaymentCount;

    private Integer amountMismatchCount;

    private Integer statusMismatchCount;

    private String failureReason;

    private Date createTime;

    private Date updateTime;
}

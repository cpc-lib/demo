package cc.ivera.entity.reconciliation;

import cc.ivera.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_reconciliation_batch")
public class ReconciliationBatch extends BaseEntity {

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
}

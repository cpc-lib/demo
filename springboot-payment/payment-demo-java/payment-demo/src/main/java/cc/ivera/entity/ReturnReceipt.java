package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_return_receipt")
public class ReturnReceipt extends BaseEntity {

    private Long refundId;

    private Long refundItemId;

    private String receiptRequestId;

    private String requestFingerprint;

    private Integer receivedQty;

    private Integer saleableQty;

    private Integer rejectedQty;

    private Long operatorId;

    private String operatorName;

    private String reason;
}


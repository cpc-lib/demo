package cc.ivera.entity.reconciliation;

import cc.ivera.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_reconciliation_detail")
public class ReconciliationDetail extends BaseEntity {

    private String batchNo;

    private String orderNo;

    private String transactionId;

    private String tradeType;

    private Integer channelAmount;

    private Integer localAmount;

    private String channelStatus;

    private String localStatus;

    private String matchStatus;

    private String discrepancyType;

    private Date tradeTime;
}

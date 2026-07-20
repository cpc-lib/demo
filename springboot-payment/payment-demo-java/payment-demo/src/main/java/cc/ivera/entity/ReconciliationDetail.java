package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_reconciliation_detail")
public class ReconciliationDetail extends BaseEntity {

    private Long reconciliationId;

    private String diffType;

    private String orderNo;

    private String transactionId;

    private Integer channelAmount;

    private Integer localAmount;

    private String channelStatus;

    private String localStatus;

    private Integer diffAmount;

    private Date channelTradeTime;

    private Date localTradeTime;

    private String remark;
}

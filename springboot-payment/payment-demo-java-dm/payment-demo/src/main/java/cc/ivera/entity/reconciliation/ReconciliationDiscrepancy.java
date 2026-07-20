package cc.ivera.entity.reconciliation;

import cc.ivera.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_reconciliation_discrepancy")
public class ReconciliationDiscrepancy extends BaseEntity {

    private String batchNo;

    private Long detailId;

    private String discrepancyType;

    private String status;

    private String resolveRemark;

    private Date resolvedTime;

    private String resolvedBy;
}

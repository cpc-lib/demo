package cc.ivera.vo.reconciliation;

import lombok.Data;

@Data
public class ReconciliationSummaryVO {

    private Integer todayBatchCount;

    private Integer todayCompletedCount;

    private Integer todayFailedCount;

    private Integer pendingDiscrepancyCount;

    private Long last7DaysTransactionAmount;

    private Long last7DaysMatchedAmount;

    private Integer last7DaysDiscrepancyCount;
}

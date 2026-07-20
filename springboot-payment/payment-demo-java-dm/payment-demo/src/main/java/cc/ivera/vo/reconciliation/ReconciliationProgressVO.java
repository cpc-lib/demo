package cc.ivera.vo.reconciliation;

import lombok.Data;

@Data
public class ReconciliationProgressVO {

    private String batchNo;

    private String status;

    private String currentStep;

    private Integer progressPercent;

    private String message;
}

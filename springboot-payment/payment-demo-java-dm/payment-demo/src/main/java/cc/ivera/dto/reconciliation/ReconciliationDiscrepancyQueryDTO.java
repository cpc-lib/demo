package cc.ivera.dto.reconciliation;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ReconciliationDiscrepancyQueryDTO {

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    private String status;

    private String discrepancyType;

    private Integer pageNum = 1;

    private Integer pageSize = 50;
}

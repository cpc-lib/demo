package cc.ivera.dto.reconciliation;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ReconciliationDetailQueryDTO {

    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    private String matchStatus;

    private String discrepancyType;

    private String tradeType;

    private Integer pageNum = 1;

    private Integer pageSize = 50;
}

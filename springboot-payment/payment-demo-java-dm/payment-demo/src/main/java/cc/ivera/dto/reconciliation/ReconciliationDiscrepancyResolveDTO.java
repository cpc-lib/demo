package cc.ivera.dto.reconciliation;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ReconciliationDiscrepancyResolveDTO {

    @NotBlank(message = "处理备注不能为空")
    private String resolveRemark;
}

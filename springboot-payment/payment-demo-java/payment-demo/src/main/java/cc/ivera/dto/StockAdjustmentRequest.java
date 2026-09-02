package cc.ivera.dto;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class StockAdjustmentRequest {

    @NotBlank(message = "库存调整请求号不能为空")
    @Size(max = 64, message = "库存调整请求号不能超过64个字符")
    private String requestId;

    @NotNull(message = "库存调整数量不能为空")
    private Integer delta;

    @NotBlank(message = "库存调整原因不能为空")
    @Size(max = 255, message = "库存调整原因不能超过255个字符")
    private String reason;

    @AssertTrue(message = "库存调整数量不能为0")
    public boolean isDeltaNonZero() {
        return delta != null && delta != 0;
    }
}

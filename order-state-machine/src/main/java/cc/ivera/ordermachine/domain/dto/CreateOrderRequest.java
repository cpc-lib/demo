package cc.ivera.ordermachine.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    private String remark;
}

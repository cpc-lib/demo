package cc.ivera.ordermachine.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransitRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "事件不能为空")
    private String event;

    @NotBlank(message = "操作人不能为空")
    private String operator;

    private String remark;
}

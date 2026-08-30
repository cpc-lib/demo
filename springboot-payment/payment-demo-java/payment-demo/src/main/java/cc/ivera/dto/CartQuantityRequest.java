package cc.ivera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartQuantityRequest {

    @NotNull(message = "课程数量不能为空")
    @Min(value = 1, message = "课程数量不能小于1")
    @Max(value = 99, message = "课程数量不能大于99")
    private Integer quantity;
}

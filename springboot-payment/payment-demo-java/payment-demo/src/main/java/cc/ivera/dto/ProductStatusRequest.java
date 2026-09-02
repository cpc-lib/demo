package cc.ivera.dto;

import cc.ivera.enums.ProductStatus;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ProductStatusRequest {

    @NotNull(message = "商品状态不能为空")
    private ProductStatus status;

    @NotNull(message = "商品版本不能为空")
    @Min(value = 0, message = "商品版本不能小于0")
    private Integer version;
}

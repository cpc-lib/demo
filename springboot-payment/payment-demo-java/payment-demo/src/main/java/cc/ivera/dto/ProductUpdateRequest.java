package cc.ivera.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 20, message = "商品名称长度不能超过20个字符")
    private String title;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格不能小于0")
    private Integer price;

    @NotNull(message = "商品版本不能为空")
    @Min(value = 0, message = "商品版本不能小于0")
    private Integer version;
}

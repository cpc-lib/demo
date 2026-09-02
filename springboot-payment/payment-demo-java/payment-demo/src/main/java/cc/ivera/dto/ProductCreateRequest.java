package cc.ivera.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 20, message = "商品名称长度不能超过20个字符")
    private String title;

    @NotNull(message = "商品价格不能为空")
    @Min(value = 0, message = "商品价格不能小于0")
    private Integer price;

    @Min(value = 0, message = "初始库存不能小于0")
    private Integer initialStock = 0;
}

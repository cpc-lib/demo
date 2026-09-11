package cc.ivera.entity;

import cc.ivera.enums.ProductStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_product")
public class Product extends BaseEntity {

    private String title; //商品名称

    private Integer price; //价格（分）

    private ProductStatus status;

    @TableField(exist = false)
    private Long skuId;

    @TableField(exist = false)
    private String skuName;

    @TableField(exist = false)
    private String skuSpec;

    @TableField(exist = false)
    private String skuStatus;

    @TableField(exist = false)
    private Integer availableStock;

    @TableField(exist = false)
    private Integer lockedStock;

    @TableField(exist = false)
    private Integer soldStock;

    private Integer version;
}

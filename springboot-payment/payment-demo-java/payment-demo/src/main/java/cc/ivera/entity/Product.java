package cc.ivera.entity;

import cc.ivera.enums.ProductStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_product")
public class Product extends BaseEntity {

    private String title; //商品名称

    private Integer price; //价格（分）

    private ProductStatus status;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Integer version;
}

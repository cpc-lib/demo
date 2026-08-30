package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItem extends BaseEntity {

    private Long orderId;

    private Long productId;

    private String productTitle;

    private Integer unitPrice;

    private Integer quantity;

    private Integer subtotal;
}

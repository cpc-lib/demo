package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart_item")
public class CartItem extends BaseEntity {

    private Long cartId;

    private Long productId;

    private Integer quantity;
}

package cc.ivera.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemView {

    private Long productId;

    private String productTitle;

    private Integer unitPrice;

    private Integer quantity;

    private Integer subtotal;
}

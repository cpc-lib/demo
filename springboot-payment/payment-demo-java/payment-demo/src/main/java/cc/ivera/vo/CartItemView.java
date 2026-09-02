package cc.ivera.vo;

import cc.ivera.enums.ProductStatus;
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

    private ProductStatus productStatus;

    private Integer availableStock;

    private Boolean purchasable;

    private String unavailableReason;
}

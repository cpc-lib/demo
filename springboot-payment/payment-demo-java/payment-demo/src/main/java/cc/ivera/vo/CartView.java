package cc.ivera.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CartView {

    private List<CartItemView> items;

    private Integer distinctCount;

    private Integer totalQuantity;

    private Integer totalAmount;
}

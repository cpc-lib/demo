package cc.ivera.entity;

import cc.ivera.enums.InventoryStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItem extends BaseEntity {

    private Long orderId;

    private Long productId;

    private Long skuId;

    private String productTitle;

    private Integer unitPrice;

    private Integer quantity;

    private Integer subtotal;

    private String productNameSnapshot;

    private String skuNameSnapshot;

    private String skuSpecSnapshot;

    private Long originalUnitAmount;

    private Long dealUnitAmount;

    private Long originalTotalAmount;

    private Long discountAmount;

    private Long payAmount;

    private Integer refundFrozenQty;

    private Integer refundedQty;

    private Long refundFrozenAmount;

    private Long refundedAmount;

    private Integer shippedQty;

    private Integer cancelConfirmedQty;

    private Integer receivedQty;

    private Integer restockedQty;

    private InventoryStatus inventoryStatus;

    private Integer refundedQuantity;
}

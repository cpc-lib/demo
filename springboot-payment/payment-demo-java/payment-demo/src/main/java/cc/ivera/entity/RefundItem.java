package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_refund_item")
public class RefundItem extends BaseEntity {

    private Long refundId;

    private Long orderItemId;

    private Long productId;

    private Long skuId;

    private String productTitle;

    private Integer unitPrice;

    private Integer quantity;

    private Long refundAmount;

    private String quotaKind;

    private Integer receivedQty;

    private Integer acceptedQty;

    private Integer restockAuthorizedQty;

    private Integer restockedQty;

    private String status;
}


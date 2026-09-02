package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_inventory_operation")
public class InventoryOperation extends BaseEntity {

    private String businessKey;

    private Long productId;

    private String operationType;

    private String orderNo;

    private String refundNo;

    private Integer availableDelta;

    private Integer lockedDelta;

    private Integer soldDelta;

    private Integer availableBefore;

    private Integer availableAfter;

    private Integer lockedBefore;

    private Integer lockedAfter;

    private Integer soldBefore;

    private Integer soldAfter;

    private Long operatorId;

    private String operatorName;

    private String reason;
}

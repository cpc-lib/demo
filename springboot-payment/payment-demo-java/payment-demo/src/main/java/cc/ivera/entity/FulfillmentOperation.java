package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_fulfillment_operation")
public class FulfillmentOperation extends BaseEntity {

    private String orderNo;

    private Long orderItemId;

    private String requestId;

    private String requestFingerprint;

    private String operationType;

    private Integer quantity;

    private Integer beforeQuantity;

    private Integer afterQuantity;

    private Long operatorId;

    private String operatorName;

    private String reason;
}


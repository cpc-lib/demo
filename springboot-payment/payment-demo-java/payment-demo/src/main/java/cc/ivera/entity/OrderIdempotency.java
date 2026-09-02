package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_order_idempotency")
public class OrderIdempotency extends BaseEntity {

    private Long userId;

    private String idempotencyKey;

    private String requestFingerprint;

    private Long orderId;

    private String status;

    private Date expiresAt;

    private Date completedAt;
}

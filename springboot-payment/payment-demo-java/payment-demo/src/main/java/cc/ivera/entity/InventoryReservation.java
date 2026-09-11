package cc.ivera.entity;

import cc.ivera.enums.ReservationStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_inventory_reservation")
public class InventoryReservation extends BaseEntity {

    private String reservationNo;

    private String orderNo;

    private Long orderItemId;

    private Long skuId;

    private Integer quantity;

    private ReservationStatus status;

    private Date expiresAt;

    private Date committedAt;

    private Date releasedAt;
}


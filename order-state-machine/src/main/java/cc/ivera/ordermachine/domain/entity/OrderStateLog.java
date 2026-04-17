package cc.ivera.ordermachine.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_state_log")
public class OrderStateLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private String businessType;

    private String fromStatus;

    private String event;

    private String toStatus;

    private String operator;

    private String remark;

    private LocalDateTime createTime;
}

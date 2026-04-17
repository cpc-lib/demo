package cc.ivera.ordermachine.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_state_transition")
public class OrderStateTransition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessType;

    private String currentStatus;

    private String event;

    private String nextStatus;

    private Integer isEnabled;

    private String rollbackMode;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

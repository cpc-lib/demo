package cc.ivera.ordermachine.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String businessType;

    private String status;

    private String beforeRefundStatus;

    private BigDecimal amount;

    @Version
    private Integer version;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

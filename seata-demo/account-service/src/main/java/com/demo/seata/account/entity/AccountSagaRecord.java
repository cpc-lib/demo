package com.demo.seata.account.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SAGA 模式账户操作与补偿幂等记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("account_saga_record")
public class AccountSagaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务唯一Key (例如 orderNo)
     */
    @TableId
    private String businessKey;

    /**
     * 用户编码
     */
    private String userId;

    /**
     * 扣减金额
     */
    private BigDecimal amount;

    /**
     * 正向状态: 0-未执行, 1-成功, 2-失败
     */
    private Integer forwardStatus;

    /**
     * 补偿状态: 0-未补偿, 1-已补偿
     */
    private Integer compensateStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

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
 * TCC 模式账户预留冻结记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("account_freeze_tbl")
public class AccountFreeze implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 全局事务 XID
     */
    @TableId
    private String xid;

    /**
     * 分支事务 ID
     */
    private Long branchId;

    /**
     * 用户编码
     */
    private String userId;

    /**
     * 冻结金额
     */
    private BigDecimal amount;

    /**
     * 状态: 1-TRY(预留), 2-CONFIRM(已扣减), 3-CANCEL(已回滚释放)
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

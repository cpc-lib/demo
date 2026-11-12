package com.demo.seata.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 账户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("account_tbl")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户编码
     */
    private String userId;

    /**
     * 资产总额
     */
    private BigDecimal totalAmount;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额 (TCC 模式使用)
     */
    private BigDecimal frozenAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

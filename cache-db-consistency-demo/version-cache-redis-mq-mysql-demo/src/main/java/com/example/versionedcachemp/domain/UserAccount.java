package com.example.versionedcachemp.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 用户账户表（带 version 字段）。
 */
@Data
@TableName("user_account")
public class UserAccount {

    @TableId
    private Long id;

    private String username;

    private BigDecimal balance;

    /**
     * 手动维护版本号，每次更新 +1。
     */
    private Long version;

    private Timestamp updateTime;
}

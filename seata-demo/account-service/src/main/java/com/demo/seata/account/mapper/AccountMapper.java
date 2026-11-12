package com.demo.seata.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 账户Mapper (SQL映射使用 XML 方式处理)
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * AT / XA 模式扣减余额 (基于数据库原子更新排他锁，防止并发超扣)
     */
    int debitBalance(@Param("userId") String userId, @Param("money") BigDecimal money);

    /**
     * TCC 模式 Try 阶段: 预留/冻结余额
     */
    int freezeBalance(@Param("userId") String userId, @Param("money") BigDecimal money);

    /**
     * TCC 模式 Confirm 阶段: 真正扣减 (扣减冻结金额)
     */
    int deductFrozenBalance(@Param("userId") String userId, @Param("money") BigDecimal money);

    /**
     * TCC 模式 Cancel 阶段: 释放预留金额 (减少冻结金额，恢复可用余额)
     */
    int unfreezeBalance(@Param("userId") String userId, @Param("money") BigDecimal money);

    /**
     * SAGA 模式补偿: 返还余额
     */
    int compensateBalance(@Param("userId") String userId, @Param("money") BigDecimal money);
}

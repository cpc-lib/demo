package com.demo.seata.api.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.math.BigDecimal;

/**
 * TCC 模式 - 账户服务 RPC 接口
 * 采用 Seata TCC Fence (防悬挂、空回滚、幂等控制)
 */
@LocalTCC
public interface AccountTccDubboService {

    /**
     * 一阶段 Try: 预留/冻结账户余额
     *
     * @param actionContext 事务上下文
     * @param userId 用户编码
     * @param amount 扣减金额
     * @return 预留成功返回 true
     */
    @TwoPhaseBusinessAction(name = "AccountTccDubboService", commitMethod = "commit", rollbackMethod = "rollback", useTCCFence = true)
    boolean prepare(BusinessActionContext actionContext,
                    @BusinessActionContextParameter(paramName = "userId") String userId,
                    @BusinessActionContextParameter(paramName = "amount") BigDecimal amount);

    /**
     * 二阶段 Confirm: 真正扣减冻结金额
     * 需保证幂等性
     *
     * @param actionContext 事务上下文
     * @return 成功返回 true
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * 二阶段 Cancel: 释放预留冻结金额，恢复可用余额
     * 需保证幂等性、防悬挂、空回滚
     *
     * @param actionContext 事务上下文
     * @return 成功返回 true
     */
    boolean rollback(BusinessActionContext actionContext);
}

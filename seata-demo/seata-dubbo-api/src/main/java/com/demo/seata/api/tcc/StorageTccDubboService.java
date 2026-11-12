package com.demo.seata.api.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * TCC 模式 - 库存服务 RPC 接口
 * 采用 Seata TCC Fence (防悬挂、空回滚、幂等控制)
 */
@LocalTCC
public interface StorageTccDubboService {

    /**
     * 一阶段 Try: 预留/冻结库存
     *
     * @param actionContext 事务上下文
     * @param commodityCode 商品编码
     * @param count 冻结扣减数量
     * @return 预留成功返回 true
     */
    @TwoPhaseBusinessAction(name = "StorageTccDubboService", commitMethod = "commit", rollbackMethod = "rollback", useTCCFence = true)
    boolean prepare(BusinessActionContext actionContext,
                    @BusinessActionContextParameter(paramName = "commodityCode") String commodityCode,
                    @BusinessActionContextParameter(paramName = "count") int count);

    /**
     * 二阶段 Confirm: 真正扣减冻结库存
     * 需保证幂等性
     *
     * @param actionContext 事务上下文
     * @return 成功返回 true
     */
    boolean commit(BusinessActionContext actionContext);

    /**
     * 二阶段 Cancel: 释放预留冻结库存，回滚到可用库存
     * 需保证幂等性、防悬挂、空回滚
     *
     * @param actionContext 事务上下文
     * @return 成功返回 true
     */
    boolean rollback(BusinessActionContext actionContext);
}

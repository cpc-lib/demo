package com.demo.seata.api.saga;

import java.math.BigDecimal;

/**
 * SAGA 模式 - 账户服务正向扣减与逆向补偿 RPC 接口
 */
public interface AccountSagaDubboService {

    /**
     * SAGA 正向行为: 扣减余额
     *
     * @param businessKey 业务全局唯一Key (如 orderNo)
     * @param userId 用户编码
     * @param amount 扣减金额
     * @return 执行成功返回 true
     */
    boolean reduceBalance(String businessKey, String userId, BigDecimal amount);

    /**
     * SAGA 补偿行为: 退还余额
     * 需保证幂等性与空补偿防悬挂处理
     *
     * @param businessKey 业务全局唯一Key (如 orderNo)
     * @param userId 用户编码
     * @param amount 补偿金额
     * @return 补偿成功返回 true
     */
    boolean compensateBalance(String businessKey, String userId, BigDecimal money);
}

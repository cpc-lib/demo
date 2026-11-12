package com.demo.seata.api.at;

import java.math.BigDecimal;

/**
 * AT 模式 - 账户服务 RPC 接口
 */
public interface AccountDubboService {

    /**
     * 扣减账户余额
     *
     * @param userId 用户编码
     * @param money 扣减金额
     */
    void debit(String userId, BigDecimal money);
}

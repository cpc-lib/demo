package com.demo.seata.api.xa;

import java.math.BigDecimal;

/**
 * XA 模式 - 账户服务 RPC 接口
 */
public interface AccountXaDubboService {

    /**
     * XA 模式扣减账户余额
     *
     * @param userId 用户编码
     * @param money 扣减金额
     */
    void debit(String userId, BigDecimal money);
}

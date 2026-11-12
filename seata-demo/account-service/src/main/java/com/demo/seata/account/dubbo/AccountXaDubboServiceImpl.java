package com.demo.seata.account.dubbo;

import com.demo.seata.account.mapper.AccountMapper;
import com.demo.seata.api.xa.AccountXaDubboService;
import com.demo.seata.common.exception.BusinessException;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * XA 模式 - 账户服务 RPC 实现
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class AccountXaDubboServiceImpl implements AccountXaDubboService {

    private final AccountMapper accountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(String userId, BigDecimal money) {
        String xid = RootContext.getXID();
        log.info("[XA模式 - 账户扣款] 开始执行, XID: {}, 用户: {}, 金额: {}", xid, userId, money);

        // 数据库级别 XA 2PC 资源保护
        int affected = accountMapper.debitBalance(userId, money);
        if (affected <= 0) {
            log.error("[XA模式 - 账户扣款] 失败: 用户 {} 可用余额不足，需扣减: {}", userId, money);
            throw new BusinessException("XA模式可用余额不足: " + userId);
        }

        log.info("[XA模式 - 账户扣款] 成功, XID: {}, 用户: {}, 金额: {}", xid, userId, money);
    }
}

package com.demo.seata.account.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.seata.account.entity.AccountFreeze;
import com.demo.seata.account.mapper.AccountFreezeMapper;
import com.demo.seata.account.mapper.AccountMapper;
import com.demo.seata.api.tcc.AccountTccDubboService;
import com.demo.seata.common.exception.BusinessException;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TCC 模式 - 账户服务 RPC 实现
 * 实现了防悬挂、空回滚、二阶段幂等控制
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class AccountTccDubboServiceImpl implements AccountTccDubboService {

    private final AccountMapper accountMapper;
    private final AccountFreezeMapper accountFreezeMapper;

    private static final int STATUS_TRY = 1;
    private static final int STATUS_CONFIRM = 2;
    private static final int STATUS_CANCEL = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean prepare(BusinessActionContext actionContext, String userId, BigDecimal amount) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 账户Try] 开始, XID: {}, branchId: {}, 用户: {}, 金额: {}", xid, branchId, userId, amount);

        // 1. 防悬挂判断: 检查是否已经先执行了 Cancel (空回滚标记)
        LambdaQueryWrapper<AccountFreeze> query = new LambdaQueryWrapper<>();
        query.eq(AccountFreeze::getXid, xid).eq(AccountFreeze::getBranchId, branchId);
        AccountFreeze freezeRecord = accountFreezeMapper.selectOne(query);
        if (freezeRecord != null) {
            log.warn("[TCC模式 - 账户Try] 检测到 Cancel 已提前执行，防悬挂拒绝执行 Try! XID: {}", xid);
            return false;
        }

        // 2. 预留资源: 冻结余额 (可用余额 - amount, 冻结金额 + amount)
        int affected = accountMapper.freezeBalance(userId, amount);
        if (affected <= 0) {
            log.error("[TCC模式 - 账户Try] 预留冻结金额失败: 用户 {} 可用余额不足", userId);
            throw new BusinessException("可用余额不足，预留冻结失败: " + userId);
        }

        // 3. 记录冻结状态表 (记录为 TRY)
        AccountFreeze record = AccountFreeze.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .amount(amount)
                .status(STATUS_TRY)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        accountFreezeMapper.insert(record);

        log.info("[TCC模式 - 账户Try] 成功预留冻结金额, XID: {}, branchId: {}", xid, branchId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 账户Confirm] 开始, XID: {}, branchId: {}", xid, branchId);

        // 1. 幂等性校验: 查询冻结记录
        LambdaQueryWrapper<AccountFreeze> query = new LambdaQueryWrapper<>();
        query.eq(AccountFreeze::getXid, xid).eq(AccountFreeze::getBranchId, branchId);
        AccountFreeze freezeRecord = accountFreezeMapper.selectOne(query);

        if (freezeRecord == null) {
            log.warn("[TCC模式 - 账户Confirm] 未找到冻结记录(可能Try未执行完成或已提交), XID: {}", xid);
            return true;
        }

        if (freezeRecord.getStatus() == STATUS_CONFIRM) {
            log.info("[TCC模式 - 账户Confirm] 已处于 CONFIRM 状态，幂等放行, XID: {}", xid);
            return true;
        }

        // 2. 扣减冻结金额 (真正扣减)
        accountMapper.deductFrozenBalance(freezeRecord.getUserId(), freezeRecord.getAmount());

        // 3. 更新冻结记录状态为 CONFIRM (按 xid + branchId 复合主键精确更新)
        LambdaUpdateWrapper<AccountFreeze> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AccountFreeze::getXid, xid)
                .eq(AccountFreeze::getBranchId, branchId)
                .set(AccountFreeze::getStatus, STATUS_CONFIRM)
                .set(AccountFreeze::getUpdateTime, LocalDateTime.now());
        accountFreezeMapper.update(null, updateWrapper);

        log.info("[TCC模式 - 账户Confirm] 成功扣减冻结金额, XID: {}, branchId: {}", xid, branchId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 账户Cancel] 开始, XID: {}, branchId: {}", xid, branchId);

        // 1. 查询冻结记录
        LambdaQueryWrapper<AccountFreeze> query = new LambdaQueryWrapper<>();
        query.eq(AccountFreeze::getXid, xid).eq(AccountFreeze::getBranchId, branchId);
        AccountFreeze freezeRecord = accountFreezeMapper.selectOne(query);

        // 2. 空回滚处理: Try 未执行，Cancel 先到达
        if (freezeRecord == null) {
            log.warn("[TCC模式 - 账户Cancel] Try阶段未执行，执行空回滚并插入防悬挂标记, XID: {}", xid);
            String userId = (String) actionContext.getActionContext("userId");
            AccountFreeze emptyRollbackMarker = AccountFreeze.builder()
                    .xid(xid)
                    .branchId(branchId)
                    .userId(userId != null ? userId : "UNKNOWN")
                    .amount(BigDecimal.ZERO)
                    .status(STATUS_CANCEL)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            accountFreezeMapper.insert(emptyRollbackMarker);
            return true;
        }

        // 3. 幂等性校验: 如果已经是 CANCEL 状态，无需重复回滚
        if (freezeRecord.getStatus() == STATUS_CANCEL) {
            log.info("[TCC模式 - 账户Cancel] 已处于 CANCEL 状态，幂等放行, XID: {}", xid);
            return true;
        }

        // 4. 释放预留冻结金额回可用余额
        accountMapper.unfreezeBalance(freezeRecord.getUserId(), freezeRecord.getAmount());

        // 5. 更新状态为 CANCEL (按 xid + branchId 复合主键精确更新)
        LambdaUpdateWrapper<AccountFreeze> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AccountFreeze::getXid, xid)
                .eq(AccountFreeze::getBranchId, branchId)
                .set(AccountFreeze::getStatus, STATUS_CANCEL)
                .set(AccountFreeze::getUpdateTime, LocalDateTime.now());
        accountFreezeMapper.update(null, updateWrapper);

        log.info("[TCC模式 - 账户Cancel] 成功释放冻结金额, XID: {}, branchId: {}", xid, branchId);
        return true;
    }
}

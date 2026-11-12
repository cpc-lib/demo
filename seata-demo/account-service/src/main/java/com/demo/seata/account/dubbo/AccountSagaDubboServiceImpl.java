package com.demo.seata.account.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.account.entity.AccountSagaRecord;
import com.demo.seata.account.mapper.AccountMapper;
import com.demo.seata.account.mapper.AccountSagaRecordMapper;
import com.demo.seata.api.saga.AccountSagaDubboService;
import com.demo.seata.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SAGA 模式 - 账户服务正向扣款与补偿 RPC 实现
 * 实现了 SAGA 幂等控制与防悬挂
 */
@Slf4j
@Service("accountSagaService")
@DubboService
@RequiredArgsConstructor
public class AccountSagaDubboServiceImpl implements AccountSagaDubboService {

    private final AccountMapper accountMapper;
    private final AccountSagaRecordMapper accountSagaRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceBalance(String businessKey, String userId, BigDecimal amount) {
        log.info("[SAGA模式 - 账户正向扣款] 开始, businessKey: {}, 用户: {}, 金额: {}", businessKey, userId, amount);

        // 1. 防悬挂判断: 检查补偿是否已先执行
        LambdaQueryWrapper<AccountSagaRecord> query = new LambdaQueryWrapper<>();
        query.eq(AccountSagaRecord::getBusinessKey, businessKey);
        AccountSagaRecord record = accountSagaRecordMapper.selectOne(query);

        if (record != null) {
            if (record.getCompensateStatus() == 1) {
                log.warn("[SAGA模式 - 账户正向扣款] 补偿已先执行，防悬挂拒绝正向执行! businessKey: {}", businessKey);
                return false;
            }
            if (record.getForwardStatus() == 1) {
                log.info("[SAGA模式 - 账户正向扣款] 正向扣款已成功执行，幂等返回 true! businessKey: {}", businessKey);
                return true;
            }
        }

        // 2. 扣减余额
        int affected = accountMapper.debitBalance(userId, amount);
        if (affected <= 0) {
            log.error("[SAGA模式 - 账户正向扣款] 余额不足: 用户 {}, 扣减金额: {}", userId, amount);
            throw new BusinessException("SAGA正向扣款余额不足: " + userId);
        }

        // 3. 记录正向执行状态
        if (record == null) {
            record = AccountSagaRecord.builder()
                    .businessKey(businessKey)
                    .userId(userId)
                    .amount(amount)
                    .forwardStatus(1)
                    .compensateStatus(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            accountSagaRecordMapper.insert(record);
        } else {
            record.setForwardStatus(1);
            record.setUpdateTime(LocalDateTime.now());
            accountSagaRecordMapper.updateById(record);
        }

        log.info("[SAGA模式 - 账户正向扣款] 成功, businessKey: {}", businessKey);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensateBalance(String businessKey, String userId, BigDecimal money) {
        log.info("[SAGA模式 - 账户补偿] 开始, businessKey: {}, 用户: {}, 金额: {}", businessKey, userId, money);

        // 1. 查询执行记录
        LambdaQueryWrapper<AccountSagaRecord> query = new LambdaQueryWrapper<>();
        query.eq(AccountSagaRecord::getBusinessKey, businessKey);
        AccountSagaRecord record = accountSagaRecordMapper.selectOne(query);

        // 2. 空补偿处理: 正向未成功执行
        if (record == null || record.getForwardStatus() != 1) {
            log.warn("[SAGA模式 - 账户补偿] 正向未成功执行，记录空补偿防悬挂标记, businessKey: {}", businessKey);
            AccountSagaRecord emptyRecord = AccountSagaRecord.builder()
                    .businessKey(businessKey)
                    .userId(userId)
                    .amount(money)
                    .forwardStatus(2) // 标记正向已失效
                    .compensateStatus(1) // 标记补偿已完成
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            if (record == null) {
                accountSagaRecordMapper.insert(emptyRecord);
            } else {
                accountSagaRecordMapper.updateById(emptyRecord);
            }
            return true;
        }

        // 3. 幂等性校验: 如果已经补偿过，无需重复退还
        if (record.getCompensateStatus() == 1) {
            log.info("[SAGA模式 - 账户补偿] 已补偿过，幂等放行! businessKey: {}", businessKey);
            return true;
        }

        // 4. 返还金额
        accountMapper.compensateBalance(userId, money);

        // 5. 更新状态为已补偿
        record.setCompensateStatus(1);
        record.setUpdateTime(LocalDateTime.now());
        accountSagaRecordMapper.updateById(record);

        log.info("[SAGA模式 - 账户补偿] 成功返还余额, businessKey: {}", businessKey);
        return true;
    }
}

package com.demo.seata.storage.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.seata.api.tcc.StorageTccDubboService;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.storage.entity.StorageFreeze;
import com.demo.seata.storage.mapper.StorageFreezeMapper;
import com.demo.seata.storage.mapper.StorageMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * TCC 模式 - 库存服务 RPC 实现
 * 实现了防悬挂、空回滚、二阶段幂等控制
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class StorageTccDubboServiceImpl implements StorageTccDubboService {

    private final StorageMapper storageMapper;
    private final StorageFreezeMapper storageFreezeMapper;

    private static final int STATUS_TRY = 1;
    private static final int STATUS_CONFIRM = 2;
    private static final int STATUS_CANCEL = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean prepare(BusinessActionContext actionContext, String commodityCode, int count) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 库存Try] 开始, XID: {}, branchId: {}, 商品: {}, 数量: {}", xid, branchId, commodityCode, count);

        // 1. 防悬挂判断: 检查是否已经先执行了 Cancel (空回滚标记)
        LambdaQueryWrapper<StorageFreeze> query = new LambdaQueryWrapper<>();
        query.eq(StorageFreeze::getXid, xid).eq(StorageFreeze::getBranchId, branchId);
        StorageFreeze freezeRecord = storageFreezeMapper.selectOne(query);
        if (freezeRecord != null) {
            log.warn("[TCC模式 - 库存Try] 检测到 Cancel 已提前执行，防悬挂拒绝执行 Try! XID: {}", xid);
            return false;
        }

        // 2. 预留资源: 冻结库存 (可用库存 - count, 冻结库存 + count)
        int affected = storageMapper.freezeStock(commodityCode, count);
        if (affected <= 0) {
            log.error("[TCC模式 - 库存Try] 冻结库存失败: 商品 {} 可用库存不足", commodityCode);
            throw new BusinessException("可用库存不足，预留冻结失败: " + commodityCode);
        }

        // 3. 记录冻结状态表 (记录为 TRY)
        StorageFreeze record = StorageFreeze.builder()
                .xid(xid)
                .branchId(branchId)
                .commodityCode(commodityCode)
                .count(count)
                .status(STATUS_TRY)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        storageFreezeMapper.insert(record);

        log.info("[TCC模式 - 库存Try] 成功预留冻结库存, XID: {}, branchId: {}", xid, branchId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean commit(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 库存Confirm] 开始, XID: {}, branchId: {}", xid, branchId);

        // 1. 幂等性校验: 查询冻结记录
        LambdaQueryWrapper<StorageFreeze> query = new LambdaQueryWrapper<>();
        query.eq(StorageFreeze::getXid, xid).eq(StorageFreeze::getBranchId, branchId);
        StorageFreeze freezeRecord = storageFreezeMapper.selectOne(query);

        if (freezeRecord == null) {
            log.warn("[TCC模式 - 库存Confirm] 未找到冻结记录(可能Try未执行完成或已提交), XID: {}", xid);
            return true;
        }

        if (freezeRecord.getStatus() == STATUS_CONFIRM) {
            log.info("[TCC模式 - 库存Confirm] 已处于 CONFIRM 状态，幂等放行, XID: {}", xid);
            return true;
        }

        // 2. 扣减冻结库存 (真正从系统抹除库存, 以冻结记录为准)
        storageMapper.deductFrozenStock(freezeRecord.getCommodityCode(), freezeRecord.getCount());

        // 3. 更新冻结记录状态为 CONFIRM (按 xid + branchId 复合主键精确更新)
        LambdaUpdateWrapper<StorageFreeze> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StorageFreeze::getXid, xid)
                .eq(StorageFreeze::getBranchId, branchId)
                .set(StorageFreeze::getStatus, STATUS_CONFIRM)
                .set(StorageFreeze::getUpdateTime, LocalDateTime.now());
        storageFreezeMapper.update(null, updateWrapper);

        log.info("[TCC模式 - 库存Confirm] 成功扣减冻结库存, XID: {}, branchId: {}", xid, branchId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(BusinessActionContext actionContext) {
        String xid = actionContext.getXid();
        long branchId = actionContext.getBranchId();
        log.info("[TCC模式 - 库存Cancel] 开始, XID: {}, branchId: {}", xid, branchId);

        // 1. 查询冻结记录
        LambdaQueryWrapper<StorageFreeze> query = new LambdaQueryWrapper<>();
        query.eq(StorageFreeze::getXid, xid).eq(StorageFreeze::getBranchId, branchId);
        StorageFreeze freezeRecord = storageFreezeMapper.selectOne(query);

        // 2. 空回滚处理: Try 未执行，Cancel 先到达
        if (freezeRecord == null) {
            log.warn("[TCC模式 - 库存Cancel] Try阶段未执行，执行空回滚并插入防悬挂标记, XID: {}", xid);
            String commodityCode = (String) actionContext.getActionContext("commodityCode");
            StorageFreeze emptyRollbackMarker = StorageFreeze.builder()
                    .xid(xid)
                    .branchId(branchId)
                    .commodityCode(commodityCode != null ? commodityCode : "UNKNOWN")
                    .count(0)
                    .status(STATUS_CANCEL)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            storageFreezeMapper.insert(emptyRollbackMarker);
            return true;
        }

        // 3. 幂等性校验: 如果已经是 CANCEL 状态，无需重复回滚
        if (freezeRecord.getStatus() == STATUS_CANCEL) {
            log.info("[TCC模式 - 库存Cancel] 已处于 CANCEL 状态，幂等放行, XID: {}", xid);
            return true;
        }

        // 4. 释放预留冻结库存回可用库存
        storageMapper.unfreezeStock(freezeRecord.getCommodityCode(), freezeRecord.getCount());

        // 5. 更新状态为 CANCEL (按 xid + branchId 复合主键精确更新)
        LambdaUpdateWrapper<StorageFreeze> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StorageFreeze::getXid, xid)
                .eq(StorageFreeze::getBranchId, branchId)
                .set(StorageFreeze::getStatus, STATUS_CANCEL)
                .set(StorageFreeze::getUpdateTime, LocalDateTime.now());
        storageFreezeMapper.update(null, updateWrapper);

        log.info("[TCC模式 - 库存Cancel] 成功释放预留库存, XID: {}, branchId: {}", xid, branchId);
        return true;
    }
}

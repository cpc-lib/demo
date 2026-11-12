package com.demo.seata.storage.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.api.saga.StorageSagaDubboService;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.storage.entity.StorageSagaRecord;
import com.demo.seata.storage.mapper.StorageMapper;
import com.demo.seata.storage.mapper.StorageSagaRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SAGA 模式 - 库存服务正向扣减与补偿 RPC 实现
 * 实现了 SAGA 幂等控制与防悬挂
 */
@Slf4j
@Service("storageSagaService")
@DubboService
@RequiredArgsConstructor
public class StorageSagaDubboServiceImpl implements StorageSagaDubboService {

    private final StorageMapper storageMapper;
    private final StorageSagaRecordMapper storageSagaRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(String businessKey, String commodityCode, int count) {
        log.info("[SAGA模式 - 库存正向扣减] 开始, businessKey: {}, 商品: {}, 数量: {}", businessKey, commodityCode, count);

        // 1. 防悬挂判断: 检查补偿是否已先执行 (如网络乱序造成补偿先于正向到达)
        LambdaQueryWrapper<StorageSagaRecord> query = new LambdaQueryWrapper<>();
        query.eq(StorageSagaRecord::getBusinessKey, businessKey);
        StorageSagaRecord record = storageSagaRecordMapper.selectOne(query);

        if (record != null) {
            if (record.getCompensateStatus() == 1) {
                log.warn("[SAGA模式 - 库存正向扣减] 补偿已先执行，防悬挂拒绝正向执行! businessKey: {}", businessKey);
                return false;
            }
            if (record.getForwardStatus() == 1) {
                log.info("[SAGA模式 - 库存正向扣减] 正向扣减已成功执行，幂等返回 true! businessKey: {}", businessKey);
                return true;
            }
        }

        // 2. 扣减库存
        int affected = storageMapper.deductStock(commodityCode, count);
        if (affected <= 0) {
            log.error("[SAGA模式 - 库存正向扣减] 库存不足: 商品 {}, 扣减数: {}", commodityCode, count);
            throw new BusinessException("SAGA正向扣减库存不足: " + commodityCode);
        }

        // 3. 记录正向执行状态
        if (record == null) {
            record = StorageSagaRecord.builder()
                    .businessKey(businessKey)
                    .commodityCode(commodityCode)
                    .count(count)
                    .forwardStatus(1)
                    .compensateStatus(0)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            storageSagaRecordMapper.insert(record);
        } else {
            record.setForwardStatus(1);
            record.setUpdateTime(LocalDateTime.now());
            storageSagaRecordMapper.updateById(record);
        }

        log.info("[SAGA模式 - 库存正向扣减] 成功, businessKey: {}", businessKey);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensateStock(String businessKey, String commodityCode, int count) {
        log.info("[SAGA模式 - 库存补偿] 开始, businessKey: {}, 商品: {}, 数量: {}", businessKey, commodityCode, count);

        // 1. 查询执行记录
        LambdaQueryWrapper<StorageSagaRecord> query = new LambdaQueryWrapper<>();
        query.eq(StorageSagaRecord::getBusinessKey, businessKey);
        StorageSagaRecord record = storageSagaRecordMapper.selectOne(query);

        // 2. 空补偿处理: 正向从未成功执行过
        if (record == null || record.getForwardStatus() != 1) {
            log.warn("[SAGA模式 - 库存补偿] 正向未成功执行，记录空补偿防悬挂标记, businessKey: {}", businessKey);
            StorageSagaRecord emptyRecord = StorageSagaRecord.builder()
                    .businessKey(businessKey)
                    .commodityCode(commodityCode)
                    .count(count)
                    .forwardStatus(2) // 标记正向已失效
                    .compensateStatus(1) // 标记补偿已完成
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            if (record == null) {
                storageSagaRecordMapper.insert(emptyRecord);
            } else {
                storageSagaRecordMapper.updateById(emptyRecord);
            }
            return true;
        }

        // 3. 幂等性校验: 如果已经补偿过，无需重复增加库存
        if (record.getCompensateStatus() == 1) {
            log.info("[SAGA模式 - 库存补偿] 已补偿过，幂等放行! businessKey: {}", businessKey);
            return true;
        }

        // 4. 增加库存
        storageMapper.compensateStock(commodityCode, count);

        // 5. 更新状态为已补偿
        record.setCompensateStatus(1);
        record.setUpdateTime(LocalDateTime.now());
        storageSagaRecordMapper.updateById(record);

        log.info("[SAGA模式 - 库存补偿] 成功恢复库存, businessKey: {}", businessKey);
        return true;
    }
}

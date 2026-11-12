package com.demo.seata.storage.dubbo;

import com.demo.seata.api.at.StorageDubboService;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.storage.mapper.StorageMapper;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AT 模式 - 库存服务 RPC 实现
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class StorageDubboServiceImpl implements StorageDubboService {

    private final StorageMapper storageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String commodityCode, int count) {
        String xid = RootContext.getXID();
        log.info("[AT模式 - 库存扣减] 开始执行, XID: {}, 商品: {}, 数量: {}", xid, commodityCode, count);

        // 1. 原子条件更新扣减库存 (防超卖)
        int affected = storageMapper.deductStock(commodityCode, count);
        if (affected <= 0) {
            log.error("[AT模式 - 库存扣减] 失败: 商品 {} 可用库存不足，扣减数: {}", commodityCode, count);
            throw new BusinessException("库存不足，扣减失败: " + commodityCode);
        }

        log.info("[AT模式 - 库存扣减] 成功, XID: {}, 商品: {}, 数量: {}", xid, commodityCode, count);
    }
}

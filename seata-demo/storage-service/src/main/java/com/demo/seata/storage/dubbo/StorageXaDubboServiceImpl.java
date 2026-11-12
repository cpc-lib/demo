package com.demo.seata.storage.dubbo;

import com.demo.seata.api.xa.StorageXaDubboService;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.storage.mapper.StorageMapper;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * XA 模式 - 库存服务 RPC 实现
 */
@Slf4j
@Service
@DubboService
@RequiredArgsConstructor
public class StorageXaDubboServiceImpl implements StorageXaDubboService {

    private final StorageMapper storageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String commodityCode, int count) {
        String xid = RootContext.getXID();
        log.info("[XA模式 - 库存扣减] 开始执行, XID: {}, 商品: {}, 数量: {}", xid, commodityCode, count);

        // 数据库级别 XA 2PC 资源保护
        int affected = storageMapper.deductStock(commodityCode, count);
        if (affected <= 0) {
            log.error("[XA模式 - 库存扣减] 失败: 商品 {} 可用库存不足，扣减数: {}", commodityCode, count);
            throw new BusinessException("XA模式库存不足，扣减失败: " + commodityCode);
        }

        log.info("[XA模式 - 库存扣减] 成功, XID: {}, 商品: {}, 数量: {}", xid, commodityCode, count);
    }
}

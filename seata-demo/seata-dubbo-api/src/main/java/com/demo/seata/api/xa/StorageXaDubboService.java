package com.demo.seata.api.xa;

/**
 * XA 模式 - 库存服务 RPC 接口
 */
public interface StorageXaDubboService {

    /**
     * XA 模式扣减库存
     *
     * @param commodityCode 商品编码
     * @param count 扣减数量
     */
    void deduct(String commodityCode, int count);
}

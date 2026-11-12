package com.demo.seata.api.at;

/**
 * AT 模式 - 库存服务 RPC 接口
 */
public interface StorageDubboService {

    /**
     * 扣减库存
     *
     * @param commodityCode 商品编码
     * @param count 扣减数量
     */
    void deduct(String commodityCode, int count);
}

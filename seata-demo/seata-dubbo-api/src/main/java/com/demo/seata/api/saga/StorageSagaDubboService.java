package com.demo.seata.api.saga;

/**
 * SAGA 模式 - 库存服务正向扣减与逆向补偿 RPC 接口
 */
public interface StorageSagaDubboService {

    /**
     * SAGA 正向行为: 扣减库存
     *
     * @param businessKey 业务全局唯一Key (如 orderNo)
     * @param commodityCode 商品编码
     * @param count 扣减数量
     * @return 执行成功返回 true
     */
    boolean reduceStock(String businessKey, String commodityCode, int count);

    /**
     * SAGA 补偿行为: 恢复库存
     * 需保证幂等性与空补偿防悬挂处理
     *
     * @param businessKey 业务全局唯一Key (如 orderNo)
     * @param commodityCode 商品编码
     * @param count 恢复数量
     * @return 补偿成功返回 true
     */
    boolean compensateStock(String businessKey, String commodityCode, int count);
}

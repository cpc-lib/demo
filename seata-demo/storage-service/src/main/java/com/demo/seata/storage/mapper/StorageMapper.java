package com.demo.seata.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.storage.entity.Storage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存Mapper (SQL映射使用 XML 方式处理)
 */
@Mapper
public interface StorageMapper extends BaseMapper<Storage> {

    /**
     * AT / XA 模式: 扣减可用库存 (通过数据库原子 update + 乐观行级排他锁保证安全)
     */
    int deductStock(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC 模式 Try 阶段: 预留库存 (扣减可用库存，增加冻结库存)
     */
    int freezeStock(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC 模式 Confirm 阶段: 真正扣减 (扣减冻结库存)
     */
    int deductFrozenStock(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * TCC 模式 Cancel 阶段: 释放预留库存 (减少冻结库存，增加可用库存)
     */
    int unfreezeStock(@Param("commodityCode") String commodityCode, @Param("count") int count);

    /**
     * SAGA 模式补偿: 增加可用库存
     */
    int compensateStock(@Param("commodityCode") String commodityCode, @Param("count") int count);
}

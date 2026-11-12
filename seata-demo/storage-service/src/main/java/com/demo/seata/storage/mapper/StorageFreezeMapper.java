package com.demo.seata.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.storage.entity.StorageFreeze;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC 冻结记录 Mapper
 */
@Mapper
public interface StorageFreezeMapper extends BaseMapper<StorageFreeze> {
}

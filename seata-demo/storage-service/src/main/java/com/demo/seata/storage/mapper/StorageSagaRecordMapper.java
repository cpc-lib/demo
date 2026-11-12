package com.demo.seata.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.storage.entity.StorageSagaRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * SAGA 操作记录 Mapper
 */
@Mapper
public interface StorageSagaRecordMapper extends BaseMapper<StorageSagaRecord> {
}

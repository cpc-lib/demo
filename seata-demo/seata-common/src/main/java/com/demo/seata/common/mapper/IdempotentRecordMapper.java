package com.demo.seata.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.common.domain.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录Mapper
 */
@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecord> {
}

package com.demo.seata.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.account.entity.AccountSagaRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * SAGA 操作记录 Mapper
 */
@Mapper
public interface AccountSagaRecordMapper extends BaseMapper<AccountSagaRecord> {
}

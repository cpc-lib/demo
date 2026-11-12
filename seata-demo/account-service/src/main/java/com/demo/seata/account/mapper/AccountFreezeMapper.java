package com.demo.seata.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.account.entity.AccountFreeze;
import org.apache.ibatis.annotations.Mapper;

/**
 * TCC 冻结记录 Mapper
 */
@Mapper
public interface AccountFreezeMapper extends BaseMapper<AccountFreeze> {
}

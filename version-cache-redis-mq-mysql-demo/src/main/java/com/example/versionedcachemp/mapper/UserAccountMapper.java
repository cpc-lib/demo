package com.example.versionedcachemp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.versionedcachemp.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * UserAccount Mapper。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 给用户加钱，版本号 +1。
     */
    int increaseBalance(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
}

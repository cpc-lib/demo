package cc.ivera.mapper;

import tk.mybatis.mapper.common.Mapper;
import cc.ivera.domain.User;

/**
 * 基础通用mapper
 */
public interface UserMapper extends Mapper<User> {
    User findById(Long id);
    User selectById(Long id);
}
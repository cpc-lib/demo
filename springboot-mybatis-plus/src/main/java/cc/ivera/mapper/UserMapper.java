package cc.ivera.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cc.ivera.pojo.User;


/**
 * @author e2607
 */
public interface UserMapper extends BaseMapper<User> {
    User findById(Long id);
}

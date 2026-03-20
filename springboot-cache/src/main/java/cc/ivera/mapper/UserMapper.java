package cc.ivera.mapper;

import org.apache.ibatis.annotations.Mapper;
import cc.ivera.domain.User;

@Mapper
public interface UserMapper extends MyBaseMapper<User> {

    User findById(Long id);

}

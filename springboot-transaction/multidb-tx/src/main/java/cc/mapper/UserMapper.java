package cc.ivera.mapper;

import cc.ivera.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper extends IBaseMapper<User> {

}

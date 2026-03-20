package cc.ivera.mapper;

import cc.ivera.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    @Select("select * from tb_user")
    List<User> selectUserList();

    @Select("select * from tb_user where id=#{id}")
    User selectOneUser(Long id);

    @Select("select * from tb_user where username=#{username}")
    User selectOneUserByName(String username);
}

package cc.ivera.dao;

import cc.ivera.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface UserDao extends BaseMapper<User> {

    /*@Insert("insert into user (id,username,password) values (null,#{username},#{password})")
    int save(User user);

    @Delete("delete from user where id=#{id}")
    int delete(Integer id);

    @Update("update user set username=#{username},password=#{password} where id=#{id}")
    int update(User user);

    @Select("select * from user")
    List<User> findAll();

    @Select("select * from user where id=#{id}")
    User findById(Integer id);*/
}
//package cc.ivera.dao;
//mybatis支持注解开发,但是可读性较xml差
//import domain.cc.ivera.User;
//import org.apache.ibatis.annotations.*;
//
//import java.util.List;
//
//@Mapper
//public interface UserDao {
//
//    @Insert("insert into user (id,username,password) values (#{id},#{username},#{password})")
//    int save(User user);
//
//    @Delete("delete from user where id=#{id}")
//    int delete(Integer id);
//
//    @Update("update user set username=#{username},password=#{password} where id=#{id}")
//    int update(User user);
//
//    @Select("select * from user")
//    List<User> findAll();
//
//    @Select("select id,username,password from user where id=#{id}")
//    User findById(Integer id);
//}

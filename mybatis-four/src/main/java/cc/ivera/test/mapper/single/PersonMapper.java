package cc.ivera.test.mapper.single;

import org.apache.ibatis.annotations.*;
import cc.ivera.domain.Person;

import java.util.List;

public interface PersonMapper {
    @Select("select *from person")
    List<Person> selectAll();

    @Insert("insert into person values(#{id},#{name},#{age})")
    int insert(Person person);

    @Update("update person set name=#{name},age=#{age} where id=#{id}")
    int update(@Param("name")String name, @Param("age")Integer age, @Param("id")Integer id);


    @Delete("delete from person where id=#{id}")
    int delete(@Param("id")Integer id);
}

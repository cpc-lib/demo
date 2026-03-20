package cc.ivera.test.mapper.dynamic.multiple.one_to_one;


import cc.ivera.domain.Person;
import org.apache.ibatis.annotations.Select;


public interface PersonMapper {
    @Select("SELECT * FROM person where id=#{id}")
    Person selectById(Integer id);
}

package cc.ivera.test.mapper.dynamic.multiple.one_to_one;

import cc.ivera.domain.Card;
import cc.ivera.domain.Person;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CardMapper {
    @Select("SELECT * FROM card")
    @Results({
            @Result(column="id",property = "id"),
            @Result(column="number",property = "number"),
            @Result(
                    column = "pid", //根据查询出的字段pid查询person表(pid的数据将会被person的id接收用作判断条件)
                    property = "p", //被包含对象在本类中的属性名
                    javaType = Person.class, //被包含对象的数据类型
                    one=@One(select = "cn.fiesacyum.mapper.multiple.one_to_one.PersonMapper.selectById")
            )
    })
    List<Card> selectAll();
}

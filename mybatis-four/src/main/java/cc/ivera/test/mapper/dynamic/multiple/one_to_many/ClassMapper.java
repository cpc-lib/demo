package cc.ivera.test.mapper.dynamic.multiple.one_to_many;

import cc.ivera.domain.Class;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ClassMapper {
    @Select("SELECT * FROM classes")
    @Results({
            @Result(column="id",property="id"),
            @Result(column="name",property="name"),
            @Result(
                    column = "id", //根据classes的字段id去查询student表
                    property = "students", //被包含对象在本类中的属性名
                    javaType=List.class, //被包含类的具体数据类型
                    //many:一对多，多对多的固定属性。select：指定某个接口中的某个方法。
                    many=@Many(select = "cn.fiesacyum.mapper.multiple.one_to_many.StudentMapper.selectByCid")
            )
    })
    List<Class> selectAll();
}

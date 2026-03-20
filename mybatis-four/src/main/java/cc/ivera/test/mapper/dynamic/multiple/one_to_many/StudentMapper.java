package cc.ivera.test.mapper.dynamic.multiple.one_to_many;

import cc.ivera.domain.Student;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StudentMapper {
    @Select("SELECT * FROM student where cid=#{cid}")
    List<Student> selectByCid(Integer cid);
}

package cc.ivera.test.mapper.dynamic.multiple.many_to_many;

import cc.ivera.domain.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseMapper {
    @Select("SELECT c.id, c.name FROM stu_cr sc, course c WHERE sc.cid=c.id AND sc.sid=#{id}")
    List<Course> selectById(Integer id);
}

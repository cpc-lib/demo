package cc.ivera.test.mapper.dynamic.multiple.many_to_many;

import cc.ivera.domain.Student;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StudentMapper {
    @Select("SELECT DISTINCT s.id,s.name,s.age FROM stu_cr sc, student s WHERE sc.sid=s.id"
            //找到每门课程对应的学生信息，并去重,筛选出name,年龄信息的理由:避免name，age为默认值null(成员变量的默认值为null)
             /*
             方法一：后期需要判断此学生的选课数是否为0，进行为0不显示学生信息的操作。
        select * from student 找出所有学生信息，包含没选课的学生信息。*/)
    @Results({
            @Result(column="id",property="id"),
            @Result(column="name",property="name"),
            @Result(column="age",property="age"),
            @Result(
                    column="id",
                    property="courses",
                    javaType=List.class,
                    many=@Many(select="cn.fiesacyum.mapper.multiple.many_to_many.CourseMapper.selectById")
            )
    })
    List<Student> selectAll();
}

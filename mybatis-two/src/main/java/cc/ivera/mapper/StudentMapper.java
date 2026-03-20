package cc.ivera.mapper;

import cc.ivera.bean.Student;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/*
    持久层接口
 */
public interface StudentMapper {
    //查询全部
    public abstract List<Student> selectAll();

    //根据id查询
    public abstract Student selectById(String id);

    //新增数据
    public abstract Integer insert(Student stu);

    //修改数据
    public abstract Integer update(Student stu);

    //删除数据
    public abstract Integer delete(String id);


    public abstract List<Student> selectByNameOrAge(@Param("name") String name, @Param("age")Integer age);

    public abstract List<Student> selectByAgeOrName(Student student);

    //根据多个id查询
    public abstract List<Student> selectByIds(List<String> list);

    //根据多个id查询
    List<Student> selectByIds2(String[] list);

    //根据多个id查询
    List<Student> selectByIds3(@Param("idList")List<String> list);

    //根据多个id查询
    List<Student> selectByIds4(@Param("idArray")String[] list);

    //多条件查询
    public abstract List<Student> selectCondition(Student stu);


}

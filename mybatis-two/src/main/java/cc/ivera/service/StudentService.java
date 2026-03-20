package cc.ivera.service;

import cc.ivera.bean.Student;

import java.util.List;

/*
    业务层接口
 */
public interface StudentService {
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

    public abstract List<Student> selectByNameOrAge(String name, Integer age);

    public abstract List<Student> selectByAgeOrName(Student student);
}

package cc.ivera.service.impl;

import cc.ivera.bean.Student;
import cc.ivera.mapper.StudentMapper;
import cc.ivera.mapper.impl.StudentMapperImpl;
import cc.ivera.service.StudentService;

import java.util.List;
/*
    业务层实现类
 */
public class StudentServiceImpl implements StudentService {

    //创建持久层对象
    private StudentMapper mapper = new StudentMapperImpl();

    @Override
    public List<Student> selectAll() {
        return mapper.selectAll();
    }

    @Override
    public Student selectById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public Integer insert(Student stu) {
        return mapper.insert(stu);
    }

    @Override
    public Integer update(Student stu) {
        return mapper.update(stu);
    }

    @Override
    public Integer delete(String id) {
        return mapper.delete(id);
    }
}

package cc.ivera.service.impl;

import cc.ivera.bean.Student;
import cc.ivera.mapper.StudentMapper;
import cc.ivera.service.StudentService;
import cc.ivera.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
/*
    业务层实现类
 */
public class StudentServiceImpl implements StudentService {

    @Override
    public List<Student> selectAll() {
       //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的selectAll方法来完成操作
        List<Student> list = mapper.selectAll();
        //释放资源
        sqlSession.close();
        //返回结果
        return list;
    }



    @Override
    public Student selectById(String id) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的selectById方法来完成操作
        Student student = mapper.selectById(id);
        return student;
    }

    @Override
    public Integer insert(Student stu) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的insert方法来完成操作
        Integer result = mapper.insert(stu);
        return result;
    }

    @Override
    public Integer update(Student stu) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的insert方法来完成操作
        Integer result = mapper.update(stu);
        return result;
    }

    @Override
    public Integer delete(String id) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的delete方法来完成操作
        Integer result = mapper.delete(id);
        return result;
    }

    @Override
    public List<Student> selectByNameOrAge(String name, Integer age) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的selectByNameOrAge方法来完成操作
        List<Student> list = mapper.selectByNameOrAge(name, age);
        return list;
    }

    @Override
    public List<Student> selectByAgeOrName(Student student) {
        //获取SqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();
        //根据StudentMapper接口,利用sqlSession的getMapper方法实现StudentMapper接口
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//多态
        //调用实现类对象的selectByAgeOrName方法来完成操作
        List<Student> list = mapper.selectByAgeOrName(student);
        return list;
    }


}

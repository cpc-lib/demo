package cc.ivera.dynamic;

import cc.ivera.bean.Student;
import cc.ivera.mapper.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DynamicTest {
    @Test
    public void selectByIds() throws Exception{
        //1.加载配置文件MyBatisConfig.xml
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过SqlSessionFactory对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过SqlSession的getMapper方法创建Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        List<String> list= new ArrayList<>();
        list.add("1");
        list.add("3");
        List<Student> students = mapper.selectByIds(list);
        for (Student student : students) {
            System.out.println(student);
        }
    }

    //不使用动态sql的缺点，条件不同，sql语句不同。灵活性差
    @Test
    public void selectCondition2() throws Exception{
        //1.加载MyBatisConfig.xml配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory工厂类对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.创建sqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过sqlSession的getMapper方法来创建mapper的事项类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);//代理模式，实现原理：反射
        Student student=new Student();
        student.setId("3");
        //student.setAge(23);
        student.setName("张三");
        List<Student> students = mapper.selectCondition(student);
        for (Student stu : students) {
            System.out.println(stu);
        }
        //释放资源
        sqlSession.close();
        is.close();
    }

    @Test
    public void selectByIds2() throws Exception{
        //1.加载配置文件MyBatisConfig.xml
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过SqlSessionFactory对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过SqlSession的getMapper方法创建Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        String[] list=new String[]{"1","3"};
        List<Student> students = mapper.selectByIds2(list);
        for (Student student : students) {
            System.out.println(student);
        }
    }


    @Test
    public void selectByIds3() throws Exception{
        //1.加载配置文件MyBatisConfig.xml
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过SqlSessionFactory对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过SqlSession的getMapper方法创建Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        List<String> list= new ArrayList<>();
        list.add("1");
        list.add("3");
        List<Student> students = mapper.selectByIds3(list);
        for (Student student : students) {
            System.out.println(student);
        }
    }

    @Test
    public void selectByIds4() throws Exception{
        //1.加载配置文件MyBatisConfig.xml
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过SqlSessionFactory对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过SqlSession的getMapper方法创建Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        String[] list=new String[]{"1","3"};
        List<Student> students = mapper.selectByIds4(list);
        for (Student student : students) {
            System.out.println(student);
        }
    }

    @Test
    public void selectCondition() throws IOException {
        //1.加载配置文件MyBatisConfig.xml
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.创建SqlSessionFactory对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过SqlSessionFactory对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.通过SqlSession的getMapper方法创建Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        Student stu = new Student("1", null, 24);
        List<Student> students = mapper.selectCondition(stu);
        for (Student student : students) {
            System.out.println(student);
        }

    }
}

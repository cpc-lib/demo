package cc.ivera.test;

import org.junit.Test;
import cc.ivera.bean.Student;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class StudentTest {
    /*
        删除功能
     */
    @Test
    public void delete() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.执行映射配置文件中的sql语句，并接收结果
        int result = sqlSession.delete("mapper.cc.ivera.StudentMapper.delete","");

        //5.提交事务
        sqlSession.commit();

        //6.处理结果
        System.out.println(result);

        //7.释放资源
        sqlSession.close();
        is.close();
    }

    /*
        修改功能
     */
    @Test
    public void update() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.执行映射配置文件中的sql语句，并接收结果
        String uuid=UUID.randomUUID().toString();
        Student stu = new Student(uuid,"周七",37);
        int result = sqlSession.update("mapper.cc.ivera.StudentMapper.update",stu);

        //5.提交事务
        sqlSession.commit();

        //6.处理结果
        System.out.println(result);

        //7.释放资源
        sqlSession.close();
        is.close();
    }

    /*
        新增功能
     */
    @Test
    public void insert() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        //SqlSession sqlSession = sqlSessionFactory.openSession();
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.执行映射配置文件中的sql语句，并接收结果
        String uuid = UUID.randomUUID().toString();
        Student stu = new Student(uuid,"周七",27);
        int result = sqlSession.insert("mapper.cc.ivera.StudentMapper.insert", stu);

        //5.提交事务
        //sqlSession.commit();

        //6.处理结果
        System.out.println(result);

        //7.释放资源
        sqlSession.close();
        is.close();
    }

    /*
        根据id查询
     */
    @Test
    public void selectById() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //4.执行映射配置文件中的sql语句，并接收结果
        Student stu = sqlSession.selectOne("mapper.cc.ivera.StudentMapper.selectById", "");

        //5.处理结果
        System.out.println(stu);

        //6.释放资源
        sqlSession.close();
        is.close();
    }

    /*
        查询全部
     */
    @Test
    public void selectAll() throws Exception{
        //1.加载核心配置文件
        //InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        InputStream is = StudentTest.class.getClassLoader().getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过SqlSession工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();

        //4.执行映射配置文件中的sql语句，并接收结果
        List<Student> list = sqlSession.selectList("mapper.cc.ivera.StudentMapper.selectAll");

        //5.处理结果
        for (Student stu : list) {
            System.out.println(stu);
        }

        //6.释放资源
        sqlSession.close();
        is.close();
    }
}

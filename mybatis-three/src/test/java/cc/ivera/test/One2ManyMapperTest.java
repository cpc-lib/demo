package cc.ivera.test;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import cc.ivera.bean.Class;
import cc.ivera.bean.Student;
import cc.ivera.mapper.One2ManyMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class One2ManyMapperTest {

    @Test
    public void selectAll() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.获取OneToManyMapper接口的实现类对象
        One2ManyMapper mapper = sqlSession.getMapper(One2ManyMapper.class);

        //5.调用实现类的方法，接收结果
        List<Class> classes = mapper.selectAll();

        //6.处理结果
        for (Class cls : classes) {
            System.out.println(cls.getId() + "," + cls.getName());
            List<Student> students = cls.getStudents();
            for (Student student : students) {
                System.out.println("\t" + student);
            }
        }

        //7.释放资源
        sqlSession.close();
        is.close();
    }

    @Test
    public void testSelectAll2() throws IOException {
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.获取OneToManyMapper接口的实现类对象
        One2ManyMapper mapper = sqlSession.getMapper(One2ManyMapper.class);

        //5.调用实现类的方法，接收结果
        List<Class> classes = mapper.selectAll2();

        //6.处理结果
        for (Class cls : classes) {
            System.out.println(cls.getId() + "," + cls.getName());
            List<Student> students = cls.getStudents();
            for (Student student : students) {
                System.out.println("\t" + student);
            }
        }

        //7.释放资源
        sqlSession.close();
        is.close();
    }
}

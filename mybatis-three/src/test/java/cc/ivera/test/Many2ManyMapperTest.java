package cc.ivera.test;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import cc.ivera.bean.Course;
import cc.ivera.bean.Student;
import cc.ivera.mapper.Many2ManyMapper;

import java.io.InputStream;
import java.util.List;

public class Many2ManyMapperTest {

    @Test
    public void selectAll() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");

        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession(true);

        //4.获取ManyToManyMapper接口的实现类对象
        Many2ManyMapper mapper = sqlSession.getMapper(Many2ManyMapper.class);

        //5.调用实现类的方法，接收结果
        List<Student> students = mapper.selectAll();

        //6.处理结果
        for (Student student : students) {
            System.out.println(student.getId() + "," + student.getName() + "," + student.getAge());
            List<Course> courses = student.getCourses();
            for (Course cours : courses) {
                System.out.println("\t" + cours);
            }
        }

        //7.释放资源
        sqlSession.close();
        is.close();
    }
}

package cc.ivera.test.multiple.many_to_many;

import cc.ivera.domain.Course;
import cc.ivera.domain.Student;
import cc.ivera.test.mapper.dynamic.multiple.many_to_many.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test4 {
    @Test
    public void testSelectAll() {
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
            List<Student> students = mapper.selectAll();
            /*方法一:处理效率相对与数据库的操作速度忙，不提倡使用
            *for (Student student : students) {
            * //判断某位学生的选课是否为空，为空不显示
                if(student.getCourses().size()!=0){
                    System.out.println(student.getId()+","+student.getName()+","+student.getAge());
                    List<Course> courses = student.getCourses();
                    for (Course cs : courses) {
                        System.out.println("\t"+cs);
                    }
                }
            }*/
            for (Student student : students) {
                System.out.println(student.getId()+","+student.getName()+","+student.getAge());
                List<Course> courses = student.getCourses();
                for (Course cours : courses) {
                    System.out.println("\t"+cours);
                }
            }
            sqlSession.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

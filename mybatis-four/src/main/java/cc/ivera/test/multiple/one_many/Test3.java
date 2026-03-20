package cc.ivera.test.multiple.one_many;

import org.junit.Test;
import cc.ivera.domain.Class;
import cc.ivera.domain.Student;
import cc.ivera.test.mapper.dynamic.multiple.one_to_many.ClassMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test3 {

    @Test
    public void testSelectAll() {
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            ClassMapper mapper = sqlSession.getMapper(ClassMapper.class);
            List<Class> classes = mapper.selectAll();
            for (Class aClass : classes) {
                System.out.println(aClass.getId()+","+aClass.getName());
                List<Student> students = aClass.getStudents();
                for (Student student : students) {
                    System.out.println("\t"+student);
                }

            }
            sqlSession.close();
            is.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}

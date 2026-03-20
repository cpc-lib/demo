package cc.ivera.test.dynamic;

import cc.ivera.domain.Person;
import cc.ivera.test.mapper.dynamic.DynamicMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test5 {
    @Test
    public void selectAll(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);
            List<Person> people = mapper.selectAll();
            for (Person person : people) {
                System.out.println(person);
            }
            sqlSession.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void delete(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);
            int result= mapper.delete(8);
            sqlSession.commit();
            if(result==1){
                System.out.println("删除成功！");
            }else{
                System.out.println("删除失败！");
            }
            sqlSession.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void update(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);
            int result= mapper.update("空气里藏着什么",6);
            sqlSession.commit();
            if(result==1){
                System.out.println("修改成功！");
            }else{
                System.out.println("修改失败！");
            }
            sqlSession.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void insert(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            DynamicMapper mapper = sqlSession.getMapper(DynamicMapper.class);
            int result= mapper.insert(new Person("22","CJ","23"));
            sqlSession.commit();
            if(result==1){
                System.out.println("添加成功！");
            }else{
                System.out.println("添加失败！");
            }
            sqlSession.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package cc.ivera.test.single;

import cc.ivera.domain.Person;
import cc.ivera.test.mapper.single.PersonMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test1 {
    @Test
    public void testSelectAll(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
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
    public void testInsert(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
            Person p=new Person(7,"周七",27);
            int result = mapper.insert(p);
            if(result==1){
                System.out.println("添加成功！");
            }else{
                System.out.println("添加失败！");
            }
            sqlSession.commit();//提交事务
            sqlSession.close();
            is.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdate(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
            int result = mapper.update("周冬雨",27,7);
            if(result==1){
                System.out.println("修改成功！");
            }else{
                System.out.println("修改失败！");
            }
            sqlSession.commit();//提交事务
            sqlSession.close();
            is.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void testDelete(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            PersonMapper mapper = sqlSession.getMapper(PersonMapper.class);
            int result = mapper.delete(7);
            if(result==1){
                System.out.println("删除成功！");
            }else{
                System.out.println("删除失败！");
            }
            sqlSession.commit();//提交事务
            sqlSession.close();
            is.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}

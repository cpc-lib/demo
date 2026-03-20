package cc.ivera.mapper.impl;

import cc.ivera.bean.Student;
import cc.ivera.mapper.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
/*
    持久层实现类
 */
public class StudentMapperImpl implements StudentMapper {

    /*
        查询全部
     */
    @Override
    public List<Student> selectAll() {
        List<Student> list = null;
        SqlSession sqlSession = null;
        InputStream is = null;
        try{
            //1.加载核心配置文件
            is = Resources.getResourceAsStream("mybatis-config.xml");

            //2.获取SqlSession工厂对象
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

            //3.通过工厂对象获取SqlSession对象
            sqlSession = sqlSessionFactory.openSession(true);

            //4.执行映射配置文件中的sql语句，并接收结果
            list = sqlSession.selectList("mapper.cc.ivera.StudentMapper.selectAll");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //5.释放资源
            if(sqlSession != null) {
                sqlSession.close();
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //6.返回结果
        return list;
    }

    /*
        根据id查询
     */
    @Override
    public Student selectById(String id) {
        Student stu = null;
        SqlSession sqlSession = null;
        InputStream is = null;
        try{
            //1.加载核心配置文件
            is = Resources.getResourceAsStream("mybatis-config.xml");

            //2.获取SqlSession工厂对象
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

            //3.通过工厂对象获取SqlSession对象
            sqlSession = sqlSessionFactory.openSession(true);

            //4.执行映射配置文件中的sql语句，并接收结果
            stu = sqlSession.selectOne("mapper.cc.ivera.StudentMapper.selectById",id);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //5.释放资源
            if(sqlSession != null) {
                sqlSession.close();
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //6.返回结果
        return stu;
    }

    /*
        新增功能
     */
    @Override
    public Integer insert(Student stu) {
        Integer result = null;
        SqlSession sqlSession = null;
        InputStream is = null;
        try{
            //1.加载核心配置文件
            is = Resources.getResourceAsStream("mybatis-config.xml");

            //2.获取SqlSession工厂对象
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

            //3.通过工厂对象获取SqlSession对象
            sqlSession = sqlSessionFactory.openSession(true);

            //4.执行映射配置文件中的sql语句，并接收结果
            result = sqlSession.insert("mapper.cc.ivera.StudentMapper.insert",stu);



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //5.释放资源
            if(sqlSession != null) {
                sqlSession.close();
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //6.返回结果
        return result;
    }

    /*
        修改功能
     */
    @Override
    public Integer update(Student stu) {
        Integer result = null;
        SqlSession sqlSession = null;
        InputStream is = null;
        try{
            //1.加载核心配置文件
            is = Resources.getResourceAsStream("mybatis-config.xml");

            //2.获取SqlSession工厂对象
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

            //3.通过工厂对象获取SqlSession对象
            sqlSession = sqlSessionFactory.openSession(true);

            //4.执行映射配置文件中的sql语句，并接收结果
            result = sqlSession.update("mapper.cc.ivera.StudentMapper.update",stu);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //5.释放资源
            if(sqlSession != null) {
                sqlSession.close();
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //6.返回结果
        return result;
    }

    /*
        删除功能
     */
    @Override
    public Integer delete(String id) {
        Integer result = null;
        SqlSession sqlSession = null;
        InputStream is = null;
        try{
            //1.加载核心配置文件
            is = Resources.getResourceAsStream("mybatis-config.xml");

            //2.获取SqlSession工厂对象
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);

            //3.通过工厂对象获取SqlSession对象
            sqlSession = sqlSessionFactory.openSession(true);

            //4.执行映射配置文件中的sql语句，并接收结果
            result = sqlSession.delete("cc.ivera.mapper1.StudentMapper.delete",id);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //5.释放资源
            if(sqlSession != null) {
                sqlSession.close();
            }
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //6.返回结果
        return result;
    }
}

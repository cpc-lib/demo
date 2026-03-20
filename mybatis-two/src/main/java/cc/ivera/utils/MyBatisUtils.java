package cc.ivera.utils;


import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class MyBatisUtils {

    private MyBatisUtils() {
    }
    private static InputStream is=null;
    private static SqlSessionFactory sqlSessionFactory=null;
    static{
        try {
            is= Resources.getResourceAsStream("mybatis-config.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //返回SqlSession对象
    public static SqlSession getSqlSession(){
        sqlSessionFactory= new SqlSessionFactoryBuilder().build(is);
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        return sqlSession;
    }
}

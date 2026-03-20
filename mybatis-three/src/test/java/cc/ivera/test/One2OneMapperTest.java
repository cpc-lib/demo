package cc.ivera.test;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import cc.ivera.bean.IDCard;
import cc.ivera.mapper.One2OneMapper;

import java.io.InputStream;
import java.util.List;

public class One2OneMapperTest {

    @Test
    public void selectAll() throws Exception{
        //1.加载核心配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2.获取SqlSession工厂对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3.通过工厂对象获取SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //4.获取OneToOneMapper接口的实现类对象
        One2OneMapper mapper = sqlSession.getMapper(One2OneMapper.class);
        //5.调用实现类的方法，接收结果
        List<IDCard> cards = mapper.selectAll();
        //6.处理结果
        for (IDCard card : cards) {
            System.out.println(card);
        }
        //7.释放资源
        sqlSession.close();
        is.close();
    }
}

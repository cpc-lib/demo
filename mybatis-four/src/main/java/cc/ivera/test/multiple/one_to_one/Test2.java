package cc.ivera.test.multiple.one_to_one;

import cc.ivera.domain.Card;
import cc.ivera.test.mapper.dynamic.multiple.one_to_one.CardMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test2 {
    @Test
    public void testSelectAll(){
        try {
            InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
            SqlSession sqlSession = sqlSessionFactory.openSession();
            CardMapper mapper = sqlSession.getMapper(CardMapper.class);
            List<Card> cards = mapper.selectAll();
            for (Card card : cards) {
                System.out.println(card);
            }
            sqlSession.close();
            is.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

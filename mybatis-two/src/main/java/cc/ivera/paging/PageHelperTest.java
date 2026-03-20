package cc.ivera.paging;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import cc.ivera.bean.Student;
import cc.ivera.mapper.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class PageHelperTest {
    @Test
    public void selectPaging() throws Exception{
        //1.加载MyBatisConfig.xml配置文件
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        //2. 创建SqlSessionFactory工厂类对象
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        //3. 通过SqlSession工厂类对象创建SqlSession对象
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        //4.通过sqlSession获取到Mapper的实现类
        StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);
        //通过pageHelper进行分页
        //显示第一页的信息
        PageHelper pageHelper=new PageHelper();
        PageMethod.startPage(3,3);
        //5.通过mapper实现类调用selectA方法显示所有信息
        List<Student> list = mapper.selectAll();
        for (Student student : list) {
            System.out.println(student);
        }

        //获取分页相关参数
        PageInfo<Student> info = new PageInfo<>(list);
        System.out.println("总条数：" + info.getTotal());
        System.out.println("总页数：" + info.getPages());
        System.out.println("当前页：" + info.getPageNum());
        System.out.println("每页显示条数：" + info.getPageSize());
        System.out.println("上一页：" + info.getPrePage());
        System.out.println("下一页：" + info.getNextPage());
        System.out.println("是否是第一页：" + info.isIsFirstPage());
        System.out.println("是否是最后一页：" + info.isIsLastPage());
        System.out.println(info);
        System.out.println(info.getList());
        //7.释放资源
        sqlSession.close();
        is.close();
    }
}

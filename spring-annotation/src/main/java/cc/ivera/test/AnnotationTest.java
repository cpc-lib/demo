package cc.ivera.test;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import cc.ivera.config.Config;
import cc.ivera.config.SpringConfig;
import cc.ivera.dao.BookDao;

import javax.sql.DataSource;

public class AnnotationTest {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext(Config.class);
        BookDao bookDao1 = app.getBean("bookDao", BookDao.class);
        BookDao bookDao2 = app.getBean("bookDao", BookDao.class);
        System.out.println(bookDao1==bookDao2);
        bookDao1.add();


        AnnotationConfigApplicationContext app2 = new AnnotationConfigApplicationContext(SpringConfig.class);
        DruidDataSource bean = (DruidDataSource) app2.getBean(DataSource.class);
        System.out.println("数据库连接用户名称:"+bean.getUsername());

        app.registerShutdownHook();
    }
}

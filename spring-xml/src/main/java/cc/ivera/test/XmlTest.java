package cc.ivera.test;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import cc.ivera.dao.BookDao;
import cc.ivera.dao.Room;
import cc.ivera.domain.Person;
import cc.ivera.service.LanguageService;
import cc.ivera.service.UserService;

public class XmlTest {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext app = new ClassPathXmlApplicationContext("applicationContext.xml");
        singleton(app);
        prototype(app);
        staticFactory(app);
        dynamicFactory(app, "getUserService");
        //暴力关闭
        //app.close();
        setter(app);
        constructor(app, "person2");
        loadPropertiesFile(app);
        componentScanByName(app);
        componentScanByType(app);
        app.registerShutdownHook();
    }

    private static void componentScanByType(ClassPathXmlApplicationContext app) {
        //根据类型进行扫描,但一个bean有多个实例，在某个实例添加注解@Primary，spring容器此时对此实例进行挂载
        Room bean = app.getBean(Room.class);
        bean.add();
    }

    private static void componentScanByName(ClassPathXmlApplicationContext app) {
        //扫描component组件根据name
        BookDao bookDao = app.getBean("bookDao", BookDao.class);
        bookDao.add();
    }

    private static void loadPropertiesFile(ClassPathXmlApplicationContext app) {
        //加载properties配置文件
        DruidDataSource dataSource = app.getBean("dataSource", DruidDataSource.class);
        System.out.println(dataSource.getDriverClassName());
    }

    private static void constructor(ClassPathXmlApplicationContext app, String person22) {
        //构造器注入
        Person person2 = app.getBean(person22, Person.class);
        System.out.println(person2);
    }

    private static void setter(ClassPathXmlApplicationContext app) {
        //setter依赖注入
        Person person = app.getBean("person", Person.class);
        System.out.println(person);
    }

    private static void dynamicFactory(ClassPathXmlApplicationContext app, String getUserService) {
        //动态工厂获取bean
        UserService dynamicFactory = (UserService) app.getBean(getUserService);
        System.out.println(dynamicFactory);
    }

    private static void staticFactory(ClassPathXmlApplicationContext app) {
        //静态工厂获取bean
        UserService staticFactory = (UserService)app.getBean("staticFactory");
        System.out.println(staticFactory);
    }

    private static void prototype(ClassPathXmlApplicationContext app) {
        //修改配置为多例模式
        LanguageService languageService1 = (LanguageService)app.getBean("multiple");
        LanguageService languageService2 = (LanguageService)app.getBean("multiple");
        System.out.println(languageService1==languageService2);
    }

    private static void singleton(ClassPathXmlApplicationContext app) {
        //无参构造方法获取bean
        //bean默认为单例singleton
        UserService userService1= (UserService)app.getBean("userService");
        UserService userService2 = (UserService)app.getBean("userService");
        System.out.println(userService1==userService2);
    }
}

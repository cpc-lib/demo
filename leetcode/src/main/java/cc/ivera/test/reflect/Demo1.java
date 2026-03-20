package cc.ivera.test.reflect;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 12/5/2021 11:21 AM
 */
public class Demo1 {

    public static void main(String[] args) throws Exception {

        //通过修改配置文件即可修改功能

        //1.创建Properties对象
        Properties properties = new Properties();

        //2.获取类加载器对象
        ClassLoader classLoader = Demo1.class.getClassLoader();

        //3.使用类加载器对象来进行读取配置文件
        InputStream resourceAsStream = classLoader.getResourceAsStream("pro.properties");

        //4.读取流进行数据的加载
        properties.load(resourceAsStream);

        //5.通过Properties对象的getProperty方法获取value信息
        String className = properties.getProperty("className");
        String methodName = properties.getProperty("methodName");

        Class<?> aClass = Class.forName(className);
        Object o = aClass.newInstance();

        Method method = aClass.getMethod(methodName);
        method.invoke(o);

    }
}

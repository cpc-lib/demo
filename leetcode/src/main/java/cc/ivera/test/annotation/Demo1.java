package cc.ivera.test.annotation;

import cc.ivera.annotation.Anno;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author e2607
 * @version 1.0
 * @description: 通过注解方式反射实现调用life方法
 * @date 12/5/2021 4:39 PM
 */
@Anno(className = "cc.ivera.model.pojo.Person", methodName = "eat")
public class Demo1 {
    public static void main(String[] args) throws NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        //获取到指定位置上面的注解
        Class<Demo1> annoTestClass = Demo1.class;
        //创建了Anno接口的实现类
        Anno annotation = annoTestClass.getAnnotation(Anno.class);

        //获取属性值
        String className = annotation.className();
        String methodName = annotation.methodName();


        //反射获取Class，创建对象，调用方法
        Class<?> aClass = Class.forName(className);
        Object o = aClass.newInstance();
        Method method = aClass.getMethod(methodName);
        method.invoke(o);
    }

}

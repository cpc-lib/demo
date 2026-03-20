package cc.ivera.test.reflect;


import cc.ivera.model.pojo.Person;

import java.lang.reflect.Constructor;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 12/5/2021 10:30 AM
 */
public class Demo5 {
    public static void main(String[] args) throws Exception {

        Class personClass = Person.class;
        //使用注意事项:传递参数类型为数据类型.class
        //如果参数类型为String，int,传递的参数应该为String.class,int.class
        Constructor constructor = personClass.getConstructor(String.class, int.class);
        //Constructor的newInstance的参数时可变参数。
        Object obj = constructor.newInstance("张三", 23);
        System.out.println(obj);


        Constructor constructor1 = personClass.getConstructor();
        Object o = constructor1.newInstance();
        System.out.println(o);


        //如果采用空参构造，一般直接调用Class的newInstance来创建对象
        Object o1 = personClass.newInstance();
        System.out.println(o1);


    }
}

package cc.ivera.test.reflect;


import cc.ivera.model.pojo.Person;
import cc.ivera.model.pojo.Student;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author e2607
 * @version 1.0
 * @description: 反射
 * @date 12/4/2021 6:34 PM
 */
public class Demo2 {
    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        //通过类.class来获取Class对象
        Class aClass0 = Person.class;
        //获取Constructor构造对象
        Constructor constructor = aClass0.getConstructor();
        //调用newInstance()获取Object对象，向下转换类型为Person
        Person person = (Person) constructor.newInstance();
        person.setAge(18);
        person.setName("hejm");
        System.out.println(person);


        //通过对象的的getClass方法来获取Class对象
        Person a = new Person();
        Class aClass1 = a.getClass();
        Constructor constructor1 = aClass1.getConstructor();
        Person b = (Person) constructor1.newInstance();
        b.setAge(20);
        b.setName("hejm");
        System.out.println(b);


        //通过Class.forName()方法来获取Class对象
        Class<?> aClass2 = Class.forName("cc.ivera.model.pojo.Person");
        Person o = (Person) aClass2.getConstructor().newInstance();
        System.out.println(o);

        /**
         * 同一Class在程序中仅仅加载一次
         */
        System.out.println(aClass0 == aClass1);
        System.out.println(aClass0 == aClass2);

        Class aClass3 = Student.class;
        System.out.println(aClass0 == aClass3);
    }
}

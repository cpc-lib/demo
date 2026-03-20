package cc.ivera.test.reflect;


import cc.ivera.model.pojo.Person;

import java.lang.reflect.Method;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 12/5/2021 10:54 AM
 */
public class Demo6 {
    public static void main(String[] args) throws Exception {
        Class<Person> personClass = Person.class;
        //获取类名并打印
        System.out.println(personClass.getName());

        //获取所有public修饰的方法，包含父类与子类的
        Method[] methods = personClass.getMethods();
        for (Method method : methods) {
            System.out.println(method);
            //获取方法的名称
            System.out.println(method.getName());
        }

        //可变参数，参数参数为方法参数类型的.class
        //如setAge(int age),在此应该传递参数int.class
        Method setAge = personClass.getMethod("setAge", int.class);
        Person person = new Person();
        setAge.invoke(person, 23);
        System.out.println(person.getAge());
    }
}

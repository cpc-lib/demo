package cc.ivera.test.reflect;


import cc.ivera.model.pojo.Animal;
import cc.ivera.model.pojo.Dog;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Demo3 {

    public static void main(String[] args) {
        testFields();
    }

    public static void testFields() {
        /***
         * getDeclaredFields与getFields的区别
         * getDeclaredFields获取本类中自己生命的字段包含public，protected，private，默认修饰的字段，不包含父类的变量
         * getFields 获取使用public修饰的字段，包含父类的public字段
         */
        try {
            System.out.println("Declared fields: ");
            Field[] fields = Dog.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                //此处结果是color, name, type, fur
                System.out.println(fields[i].getName());
            }
            System.out.println("----------------------------------");
            System.out.println("Public fields: ");
            fields = Dog.class.getFields();
            for (int i = 0; i < fields.length; i++) {
                //此处结果是color, location
                System.out.println(fields[i].getName());
            }
            System.out.println("----------------------------------");
            Dog dog = new Dog();
            dog.setAge(1);
            //继承的优点
            Method method = dog.getClass().getMethod("getAge", null);
            Integer value = (Integer) method.invoke(dog);
            //此处结果是1
            System.out.println(value);

            Animal animal = new Animal();
            //属性描述器的使用进行修改内容
            PropertyDescriptor pd = new PropertyDescriptor("age", Animal.class);
            Method writeMethod = pd.getWriteMethod();
            writeMethod.invoke(animal, 10);

            //打印动物的年龄为10
            System.out.println(animal.getAge());


            Dog dog1 = new Dog();
            //获取字段的方法
            Field field = dog1.getClass().getField("location");
            //获取对象的字段的值
            //引用类型的默认值为null
            System.out.println(field.get(dog1));
            //设置对对象，对应的字段值
            field.set(dog1, "Shanghai");
            Field type = dog1.getClass().getDeclaredField("type");
            type.setAccessible(true);
            type.set(dog1, "金毛");
            System.out.println(dog1.getLocation());
            System.out.println(dog1.getType());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

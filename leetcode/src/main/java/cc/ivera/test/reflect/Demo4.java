package cc.ivera.test.reflect;


import cc.ivera.model.pojo.Student;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射操作类,通过PropertyDescriptor更简单的操作get、set方法
 * 无需判断字段的类型，即可完成数据的修改
 *
 * @author chinoukin
 */
public class Demo4 {

    public static void main(String[] args)
            throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Student s = new Student();
        s.setName("张三");
        s.setAge(20);
        s.setFlag(true);

        String propertyName = "name";

        Object retVal = getPropertyVal(propertyName, s);
        //打印内容张三
        System.out.println(retVal);

        String newVal = "李四";
        setPropertyVal(propertyName, s, newVal);
        System.out.println(s.getName());


        String propertyName1 = "age";

        Object retVal1 = getPropertyVal(propertyName1, s);
        System.out.println(retVal1);

        Integer newVal1 = 23;
        setPropertyVal(propertyName1, s, newVal1);
        System.out.println(s.getAge());

        String propertyName2 = "flag";
        Boolean newVal2 = false;
        setPropertyVal(propertyName2, s, newVal2);
        System.out.println(s.getFlag());

    }

    private static void setPropertyVal(String propertyName, Object s, Object newVal)
            throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        PropertyDescriptor pd = new PropertyDescriptor(propertyName, s.getClass());
        Method setMethod = pd.getWriteMethod();
        setMethod.invoke(s, newVal);
    }

    private static Object getPropertyVal(String propertyName, Object s)
            throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        PropertyDescriptor pd = new PropertyDescriptor(propertyName, s.getClass());
        Method getMethod = pd.getReadMethod();
        Object retVal = getMethod.invoke(s);
        return retVal;
    }

}

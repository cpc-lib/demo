package cc.ivera.test.base.demo87;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflectionComparator {

    // 定义一个静态内部类来存储字段的变化信息
    public static class FieldChange {
        public final String fieldName;
        public final Object oldValue;
        public final Object newValue;

        public FieldChange(String fieldName, Object oldValue, Object newValue) {
            this.fieldName = fieldName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return "Field " + fieldName + " changed from " + oldValue + " to " + newValue;
        }
    }

    public static Map<String, FieldChange> compareObjects(Object obj1, Object obj2) throws IllegalAccessException {
        Map<String, FieldChange> changedFields = new HashMap<>();

        // 获取对象的类类型
        Class<?> clazz = obj1.getClass();

        // 遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            // 设置字段可访问
            field.setAccessible(true);

            // 获取第一个对象的字段值
            Object value1 = field.get(obj1);
            // 获取第二个对象的字段值
            Object value2 = field.get(obj2);

            // 比较字段值是否不同
            if (!value1.equals(value2)) {
                // 记录变化的字段和旧值、新值
                FieldChange change = new FieldChange(field.getName(), value1, value2);
                changedFields.put(field.getName(), change);
            }
        }

        return changedFields;
    }

    public static void main(String[] args) throws IllegalAccessException {
        Person person1 = new Person("Alice", 30, "123 Street");
        Person person2 = new Person("Alice", 31, "456 Avenue");

        Map<String, FieldChange> changedFields = compareObjects(person1, person2);

        // 打印变化的字段及其旧值和新值
        for (Map.Entry<String, FieldChange> entry : changedFields.entrySet()) {
            System.out.println(entry.getValue());
        }
    }
}

// 假设 Person 类与之前相同（已省略）
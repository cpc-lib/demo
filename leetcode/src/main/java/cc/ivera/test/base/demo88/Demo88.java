package cc.ivera.test.base.demo88;

import lombok.Data;

import java.lang.reflect.Field;

@Data
class UserDO {
    private String name;
    private Integer age;
    private String email;
}

@Data
class UserVO {
    private String name;
    private Integer age;
    private String email;
}

public class Demo88 {

    public static boolean areFieldsEqual(UserDO doObject, UserVO voObject) {
        if (doObject == null || voObject == null) {
            throw new IllegalArgumentException("Both objects must not be null");
        }

        // Use reflection to compare fields
        Field[] fields = voObject.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                field.setAccessible(true); // Allow access to private fields

                // Get the field name
                String fieldName = field.getName();

                // Get the value from the VO object
                Object voValue = field.get(voObject);

                // Get the corresponding value from the DO object
                Field doField = doObject.getClass().getDeclaredField(fieldName);
                doField.setAccessible(true);
                Object doValue = doField.get(doObject);

                // Compare the values
                if (voValue == null) {
                    // If VO value is null, consider it as no change
                    continue;
                } else if (!voValue.equals(doValue)) {
                    // If values are not equal, return false
                    return false;
                }
            }
            // If all fields are equal, return true
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error comparing objects", e);
        }
    }

    public static void main(String[] args) {
        UserDO userDO = new UserDO();
        userDO.setName("Alice");
        userDO.setAge(30);
        userDO.setEmail("alice@example.com");

        UserVO userVO1 = new UserVO();
        userVO1.setName(null); // Considered as no change
        userVO1.setAge(30);    // Same as DO, no change
        userVO1.setEmail("newemail@example.com"); // Different from DO

        UserVO userVO2 = new UserVO();
        userVO2.setName("Alice"); // Same as DO, no change
        userVO2.setAge(null);    // Considered as no change
        userVO2.setEmail("alice@example.com"); // Same as DO, no change

        System.out.println(areFieldsEqual(userDO, userVO1)); // Output: false
        System.out.println(areFieldsEqual(userDO, userVO2)); // Output: true
    }
}

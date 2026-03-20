package cc.ivera.test.base;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Demo16 {
    private static final String PHONE_NUMBER_REGEX = "^(?:(?:\\+|00)86)?1(?:(?:3[\\d])|(?:4[5-79])|(?:5[0-35-9])|(?:6[5-7])|(?:7[0-8])|(?:8[\\d])|(?:9[1589]))\\d{8}$";

    @Test
    public void test1() {
        String phoneNumber = "15575909531";
        Pattern compilePattern = Pattern.compile(PHONE_NUMBER_REGEX);
        Matcher matcher = compilePattern.matcher(phoneNumber);
        if (matcher.matches()) {
            System.out.println(true);
        }
        System.out.println(false);
    }
}

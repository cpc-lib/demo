package cc.ivera.test.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import cc.ivera.util.SecureUtil;

@Slf4j
public class Demo18 {
    @Test
    public void test() {
        String msg = "I Love You";
        String s1 = SecureUtil.encrypt(msg);
        System.out.println(s1);
        String s2 = SecureUtil.decrypt(s1);
        System.out.println(s2);
        Assert.assertEquals(msg, s2);
        Integer number = parseInt("12!");
        System.out.println(number);


    }


    public Integer parseInt(String numberStr) {
        Integer i = null;
        try {
            i = Integer.parseInt("12!");
        } catch (Exception e) {
            log.error("failed to parse {}", e.getMessage());
        }
        return i;
    }
}

package cc.ivera.test.base;

import java.math.BigDecimal;

public class Demo24 {
    public static void main(String[] args) {
        BigDecimal bigDecimal = new BigDecimal("1.05");
        BigDecimal pow = bigDecimal.pow(2);
        System.out.println(pow);
    }
}

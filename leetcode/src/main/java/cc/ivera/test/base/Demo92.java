package cc.ivera.test.base;

import java.math.BigDecimal;

public class Demo92 {
    public static void main(String[] args) {
        BigDecimal num1 = BigDecimal.valueOf(0.1);
        BigDecimal num2 = BigDecimal.valueOf(0.2);
        BigDecimal sum = num1.add(num2);
        System.out.println(sum);
    }
}

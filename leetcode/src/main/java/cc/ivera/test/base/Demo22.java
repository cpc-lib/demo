package cc.ivera.test.base;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Demo22 {
    public static void main(String[] args) {
        BigDecimal num = new BigDecimal("1.665");
        //四舍五入
        BigDecimal divide = num.divide(new BigDecimal(3), 6, RoundingMode.HALF_UP);
        System.out.println(divide);
    }
}

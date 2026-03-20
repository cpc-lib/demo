package cc.ivera.test.base;

import java.text.DecimalFormat;

public class Demo103 {
    public static void main(String[] args) {
        //数值123456 转换成123,456
        DecimalFormat format = new DecimalFormat("#,###.00");
        String formatNumber = format.format(123456);
        System.out.println(formatNumber);
    }
}

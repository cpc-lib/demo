package cc.ivera.test.base;

import java.util.Random;

public class Demo17 {

    public static void main(String[] args) {
        for (int i = 0; i < 100L; i++) {
            // 增加标识，OPENID默认28位
            System.out.println("jif_" + genRandomNum());
        }
    }

    public static String genRandomNum() {
        int maxNum = 37;
        int count = 0;
        char[] str = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
                'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
                'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '$'};
        StringBuilder buffer = new StringBuilder();
        Random r = new Random();
        while (count < 22) {
            int i = Math.abs(r.nextInt(maxNum));
            if (i >= 0 && i < str.length) {
                buffer.append(str[i]);
                count++;
            }
        }
        return buffer.toString();
    }

}

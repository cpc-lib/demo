package cc.ivera.test.base;

import cc.ivera.util.MoneyUtil;

public class Demo45 {
    public static void main(String[] args) {
        String abc = "93098.754";
        String s = MoneyUtil.toChinese(abc);
        System.out.println(s);
    }
}

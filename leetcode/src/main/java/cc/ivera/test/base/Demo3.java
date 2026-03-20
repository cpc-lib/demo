package cc.ivera.test.base;

import cn.hutool.core.date.DateUtil;

import java.text.ParseException;
import java.util.Date;

public class Demo3 {
    public static void main(String[] args) throws ParseException {
        Date parse = DateUtil.parse("2023-09-06");
        System.out.println(parse);

        int i = DateUtil.year(parse);
        int i1 = DateUtil.month(parse);

        int i2 = DateUtil.weekOfMonth(parse);

        System.out.println(i);
        System.out.println(i1);
        System.out.println(i2);

    }


}


package cc.ivera.test.base;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Demo80 {
    public static void main(String[] args) {
        String dateTimeString = "2024-08-06T18:19:19+08:00";

        // 使用 OffsetDateTime 解析日期时间字符串  
        OffsetDateTime odt = OffsetDateTime.parse(dateTimeString);

        // 将 OffsetDateTime 转换为 Date  
        Date date = Date.from(odt.atZoneSameInstant(ZoneId.systemDefault()).toInstant());

        // 打印结果  
        System.out.println(date);
    }
}
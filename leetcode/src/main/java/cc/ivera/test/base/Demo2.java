package cc.ivera.test.base;

import cn.hutool.core.date.DateUtil;

import java.util.Calendar;
import java.util.Date;

public class Demo2 {
    public static void main(String[] args) {
        int currentYear = DateUtil.year(new Date()); // 获取当前年份

        for (int year = currentYear - 4; year <= currentYear; year++) {
            System.out.println("Year: " + year);

            for (int month = 1; month <= 12; month++) {
                String yearMonthStr = year + "-" + String.format("%02d", month); // 构建年月字符串，补齐月份的前导零
                System.out.print(yearMonthStr + " ");

                // 解析年月字符串为日期
                Date firstDayOfMonth = DateUtil.parse(yearMonthStr + "-01");

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(firstDayOfMonth);

                // 遍历每个周
                int weekNumber = 1;
                while (calendar.get(Calendar.YEAR) == year || calendar.get(Calendar.MONTH) == month - 1) {
                    Date startOfWeek = DateUtil.beginOfWeek(calendar.getTime()); // 每周的开始日期
                    Date endOfWeek = DateUtil.endOfWeek(calendar.getTime()); // 每周的结束日期

                    System.out.print("第" + weekNumber + "周 " + DateUtil.format(startOfWeek, "yyyy-MM-dd") + " - " + DateUtil.format(endOfWeek, "yyyy-MM-dd") + " ");

                    calendar.add(Calendar.DATE, 7); // 下一周
                    weekNumber++;
                }

                System.out.println();
            }
            System.out.println();
        }
    }
}

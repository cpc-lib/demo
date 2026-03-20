package cc.ivera.test.base;

import cn.hutool.core.date.DateUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * 计算周的开始时间与结束时间
 */
public class Demo6 {
    public static void main(String[] args) {
        // 获取当前时间
        Date currentDate = new Date();

        // 获取当前时间所在周的开始日期和结束日期
        Date weekStart = DateUtil.beginOfWeek(currentDate);
        Date weekEnd = DateUtil.endOfWeek(currentDate);

        // 设置date2变量获取当前时间所在周的周末（结束日期）
        Date date2 = weekEnd;

        // 获取当前时间的年份和月份
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentDate);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH);

        // 循环遍历过去三年
        for (int i = 0; i < 3; i++) {
            int year = currentYear - i;

            // 获取当前年的第一天
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, Calendar.JANUARY);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date firstDayOfYear = calendar.getTime();

            // 获取当前年的最后一天
            calendar.set(Calendar.MONTH, Calendar.DECEMBER);
            calendar.set(Calendar.DAY_OF_MONTH, 31);
            Date lastDayOfYear = calendar.getTime();

            // 遍历每月的周
            calendar.setTime(firstDayOfYear);
            while (!calendar.getTime().after(lastDayOfYear)) {
                Date monthWeekStart = DateUtil.beginOfWeek(calendar.getTime());
                Date monthWeekEnd = DateUtil.endOfWeek(calendar.getTime());

                // 检查周结束日期是否超过当前系统时间所在月的周
                if (!monthWeekEnd.after(date2)) {
                    // 打印当前月周的开始和结束日期以及所属年份和月份
                    System.out.println("年份：" + year + "  月份：" + (calendar.get(Calendar.MONTH) + 1));
                    System.out.println(DateUtil.weekOfMonth(monthWeekStart) + "周开始日期：" + DateUtil.format(monthWeekStart, "yyyy年MM月dd日") +
                            "  周结束日期：" + DateUtil.format(monthWeekEnd, "yyyy年MM月dd日"));
                }

                // 移动到下一周
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            // 如果是当前年份，限制遍历的月份范围
            if (year == currentYear) {
                calendar.set(Calendar.MONTH, currentMonth);
            }
        }
    }
}

package cc.ivera.test.base;

import cn.hutool.core.date.DateUtil;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;

public class Demo1 {

    /**
     * 输入年月 周 计算 这周的开始时间与结束时间
     */
    @Test
    public void test1() {
        String yearMonthStr = "2024-06"; // 年月
        int weekNumber = 5; // 周数
        // 解析年月字符串为日期
        String firstDayOfMonthStr = yearMonthStr + "-01";
        Date firstDayOfMonth = DateUtil.parse(firstDayOfMonthStr);
        // 计算特定周的开始日期和结束日期
        Date startOfWeek = DateUtil.beginOfWeek(firstDayOfMonth); // 第一周的开始日期
        startOfWeek = DateUtil.offsetDay(startOfWeek, (weekNumber - 1) * 7); // weekNumber减1是因为索引从0开始
        Date endOfWeek = DateUtil.endOfWeek(startOfWeek); // 特定周的结束日期
        System.out.println(yearMonthStr + "的第" + weekNumber + "周的开始时间:" + startOfWeek);
        System.out.println(yearMonthStr + "的第" + weekNumber + "周的结束时间:" + endOfWeek);
    }

    /**
     * 时间格式化
     */
    @Test
    public void test2() {
        Date date = new Date();
        LocalDateTime localDateTime = cc.ivera.util.DateUtil.convertDateToLocalDateTime(date);
        System.out.println(localDateTime);
    }

    /**
     * 时间格式化
     */
    @Test
    public void test3() {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String formatedTime = format.format(date);
        System.out.println(formatedTime);
    }


    /**
     * 计算本日是今年某月的第几周，并计算这周的开始时间与结束时间
     */
    @Test
    public void test4() {

        LocalDateTime localDateTime = LocalDateTime.now();
        int weekOfMonth = getWeekOfMonth(localDateTime);
        LocalDateTime startOfWeek = getStartOfWeek(localDateTime);
        LocalDateTime endOfWeek = getEndOfWeek(localDateTime);

        int year = localDateTime.getYear();
        Month month = localDateTime.getMonth();
        String monthStr = month.getValue() < 10 ? "0" + month.getValue() : "" + month.getValue();

        Date currentDate = cc.ivera.util.DateUtil.toDate(localDateTime);
        String yearMonth = DateUtil.format(currentDate, "YYYY-MM");

        System.out.println("本周是" + yearMonth + "的第" + weekOfMonth + "周");

        Date begin = cc.ivera.util.DateUtil.toDate(startOfWeek);
        Date end = cc.ivera.util.DateUtil.toDate(endOfWeek);

        String time1 = cc.ivera.util.DateUtil.parseDateToStr(cc.ivera.util.DateUtil.YYYY_MM_DD_HH_MM_SS, begin);
        String time2 = cc.ivera.util.DateUtil.parseDateToStr(cc.ivera.util.DateUtil.YYYY_MM_DD_HH_MM_SS, end);


        System.out.println(yearMonth + "的第" + weekOfMonth + "周的开始时间:" + time1);
        System.out.println(yearMonth + "的第" + weekOfMonth + "周的结束时间:" + time2);

    }


    public static int getWeekOfMonth(LocalDateTime dateTime) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekOfMonth = dateTime.toLocalDate().get(weekFields.weekOfMonth());
        return weekOfMonth;
    }

    public static LocalDateTime getStartOfWeek(LocalDateTime dateTime) {
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = startOfDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return startOfWeek.withSecond(0).withNano(0); // 设置为0秒开始
    }

    public static LocalDateTime getEndOfWeek(LocalDateTime dateTime) {
        LocalDateTime endOfDay = dateTime.toLocalDate().plusDays(1).atStartOfDay().minusNanos(1); // 加一天然后减一纳秒得到当天的最后一秒
        LocalDateTime endOfWeek = endOfDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return endOfWeek.withSecond(59).withNano(0); // 设置为59秒结束
    }
}

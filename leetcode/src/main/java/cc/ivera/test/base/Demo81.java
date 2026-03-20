package cc.ivera.test.base;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.StringJoiner;

public class Demo81 {


    @Test
    public void test1() {
        String startDateStr = "2023-01-01";
        String endDateStr = "2023-03-02";

        // 解析字符串为LocalDate对象
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 计算两个日期之间的整年数
        long years = ChronoUnit.YEARS.between(startDate, endDate);
        // 计算剩余月份数（不包括整年中的月份）
        LocalDate tempDate = startDate.plusYears(years);
        long months = ChronoUnit.MONTHS.between(tempDate, endDate);

        // 计算剩余天数（不包括整年和整月中的天数）
        LocalDate finalTempDate = tempDate.plusMonths(months);
        long days = ChronoUnit.DAYS.between(finalTempDate, endDate);

        StringJoiner sj = new StringJoiner("");
        if (years > 0L) {
            sj.add(years + "年");
        }
        if (months > 0) {
            sj.add(months + "月");
        }
        if (days > 0) {
            sj.add(days + "日");
        }
        System.out.println(sj.toString());
    }


    @Test
    public void test2() {
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 11);
        LocalDateTime endDate = LocalDateTime.of(2023, 2, 28, 10, 59, 9);
        doCalculate(startDate, endDate);
    }

    public static void doCalculate(LocalDateTime startDate, LocalDateTime endDate) {
        Period period = Period.between(startDate.toLocalDate(), endDate.toLocalDate());
        long years = period.getYears();
        long months = period.getMonths();
        long days = period.getDays();

        Duration duration = Duration.between(startDate, endDate);
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder timeGap = new StringBuilder();

        if (years > 0) {
            timeGap.append(years).append("年");
        }
        if (months > 0) {
            timeGap.append(months).append("月");
        }
        if (days > 0) {
            timeGap.append(days).append("日");
        }
        if (hours > 0) {
            timeGap.append(hours).append("时");
        }
        if (minutes > 0) {
            timeGap.append(minutes).append("分");
        }
        if (seconds > 0) {
            timeGap.append(seconds).append("秒");
        }

        System.out.println(timeGap.toString());
        System.out.println(years + "年" + months + "月" + days + "日" + hours + "时" + minutes + "分" + seconds + "秒");
    }


    @Test
    public void test3() {
        BigDecimal bigDecimal = new BigDecimal("-1.00");
        int i = bigDecimal.compareTo(new BigDecimal("0.00"));
        System.out.println(i);
    }
}
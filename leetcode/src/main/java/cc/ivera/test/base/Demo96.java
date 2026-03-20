package cc.ivera.test.base;

import cc.ivera.util.DateUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

public class Demo96 {
    public static void main(String[] args) {
        // 定义开始和结束时间
        LocalDateTime start = LocalDateTime.of(2025, 6, 2, 0, 0, 0);
        LocalDateTime end = DateUtils.convertDateToLocalDateTime(DateUtils.getEnd(new Date()));
        System.out.println(gaps(start, end));

        System.out.println(plusSeconds(end, 1));
    }

    private static String gaps(LocalDateTime start, LocalDateTime end) {
        // 计算年、月、日差异（忽略时间）
        Period period = Period.between(start.toLocalDate(), end.toLocalDate());

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        String gaps = "";
        if (years != 0) {
            gaps += years + "年";
        }
        if (months != 0) {
            gaps += months + "月";
        }
        if (days != 0) {
            gaps += days + "日";
        }

        return gaps;
    }


    public static Date plusSeconds(LocalDateTime inputLocalDateTime, int seconds) {
        LocalDateTime localDateTime = inputLocalDateTime.plusSeconds(seconds);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }
}


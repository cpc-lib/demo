package cc.ivera.test.biz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class PaymentCalculator {

    /**
     * 计算从 localDateTimeStart 到 localDateTimeEnd 的应付金额（按 30 天/月 折算）
     * sum 为初始累计（通常传 BigDecimal.ZERO）
     */
    public static BigDecimal getBigDecimal1(
            BigDecimal sum,
            LocalDateTime localDateTimeStart,
            LocalDateTime localDateTimeEnd,
            BigDecimal payment) {

        if (sum == null) sum = BigDecimal.ZERO;
        if (localDateTimeStart == null || localDateTimeEnd == null || payment == null) {
            throw new IllegalArgumentException("参数不能为 null");
        }

        // 如果开始已经晚于结束，直接返回
        if (localDateTimeStart.isAfter(localDateTimeEnd)) {
            return sum.setScale(2, RoundingMode.HALF_UP);
        }

        LocalDateTime start = localDateTimeStart;
        LocalDateTime end = localDateTimeEnd;

        // 按天的单价（保留 6 位小数用于累加精度）
        BigDecimal perDay = payment.divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP);

        // 1) 如果开始时间不是月初，先结算到当月月底
        LocalDate startDate = start.toLocalDate();
        if (startDate.getDayOfMonth() != 1) {
            LocalDate endOfStartMonthDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

            // 如果结束时间在当月月底之前或等于月底 -> 直接结算到结束时间，返回
            if (!end.toLocalDate().isAfter(endOfStartMonthDate)) {
                long days = daysBetweenInclusive(start, end);
                BigDecimal part = perDay.multiply(BigDecimal.valueOf(days)).setScale(6, RoundingMode.HALF_UP);
                sum = sum.add(part);
                return sum.setScale(2, RoundingMode.HALF_UP);
            }

            // 结束时间在当月月底之后 -> 先结算 start -> 当月月底
            long daysToMonthEnd = daysBetweenInclusive(start, endOfStartMonthDate.atStartOfDay());
            BigDecimal partToMonthEnd = perDay.multiply(BigDecimal.valueOf(daysToMonthEnd)).setScale(6, RoundingMode.HALF_UP);
            sum = sum.add(partToMonthEnd);

            // 把 start 推到下个月的月初（00:00）
            LocalDate firstOfNextMonth = startDate.plusMonths(1).withDayOfMonth(1);
            start = firstOfNextMonth.atStartOfDay();

            // 若推进后已经超过结束，直接返回
            if (start.isAfter(end)) {
                return sum.setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 2) 计算整自然月数（此时 start 应该是某个月的第一天 00:00）
        YearMonth startYM = YearMonth.from(start);
        YearMonth endYM = YearMonth.from(end);

        int monthsBetween = (endYM.getYear() - startYM.getYear()) * 12 + (endYM.getMonthValue() - startYM.getMonthValue());
        int fullMonths = monthsBetween;
        // 如果结束时间是所在月的最后一天，则把最后这个月算作完整月
        if (end.toLocalDate().equals(endYM.atEndOfMonth())) {
            fullMonths = monthsBetween + 1;
        }

        if (fullMonths > 0) {
            BigDecimal fullMonthPayment = payment.multiply(BigDecimal.valueOf(fullMonths));
            sum = sum.add(fullMonthPayment);
            start = start.plusMonths(fullMonths); // 把 start 推过这些整月
        }

        // 3) 结算尾部不足一个月的天数（若还有剩余）
        if (!start.isAfter(end)) {
            long tailDays = daysBetweenInclusive(start, end);
            BigDecimal tailPart = perDay.multiply(BigDecimal.valueOf(tailDays)).setScale(6, RoundingMode.HALF_UP);
            sum = sum.add(tailPart);
        }

        // 最终保留 2 位小数返回
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    /** 包含首尾的天数计算（例如 2025-01-02 到 2025-01-31 返回 30） */
    private static long daysBetweenInclusive(LocalDateTime a, LocalDateTime b) {
        return ChronoUnit.DAYS.between(a.toLocalDate(), b.toLocalDate()) + 1L;
    }

    // 测试用 main（可删）
    public static void main(String[] args) {
        BigDecimal sum = BigDecimal.ZERO;
        LocalDateTime start = LocalDateTime.of(2025, 1, 2, 0, 0,0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 30, 23, 59,59);
        BigDecimal payment = BigDecimal.valueOf(1000); // 每月金额示例
        BigDecimal result = getBigDecimal1(sum, start, end, payment);
        System.out.println(result); // 输出示例
    }
}

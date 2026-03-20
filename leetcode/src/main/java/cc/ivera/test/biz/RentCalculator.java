package cc.ivera.test.biz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RentCalculator {

    /**
     * 按照24小时1天 自然月租赁计算租金
     *
     * @param sum
     * @param localDateTimeStart
     * @param localDateTimeEnd
     * @param payment
     * @return
     */
    private static BigDecimal getBigDecimalPlus(BigDecimal sum, LocalDateTime localDateTimeStart, LocalDateTime localDateTimeEnd, BigDecimal payment) {
        // 判断是否刚好是 n 个自然月
        LocalDateTime temp = localDateTimeStart;
        int months = 0;
        while (temp.isBefore(localDateTimeEnd)) {
            temp = temp.plusMonths(1);
            months++;
            if (temp.minusSeconds(1).equals(localDateTimeEnd)) {
                sum = sum.add(payment.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP));
                return sum;
            }
        }
        // 不是整月：判断是否是月初
        if (localDateTimeStart.getDayOfMonth() != 1) {
            // 计算从开始日期到当月月底的天数
            LocalDateTime endOfStartMonth = localDateTimeStart.plusMonths(1).withDayOfMonth(1).minusSeconds(1);
            if (endOfStartMonth.isAfter(localDateTimeEnd)) {
                long beforeDays = ChronoUnit.DAYS.between(localDateTimeStart, localDateTimeEnd) + 1; // 含当天
                BigDecimal part = payment.multiply(BigDecimal.valueOf(beforeDays)).setScale(2, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                sum = sum.add(part);
                return sum;
            } else {
                long beforeDays = ChronoUnit.DAYS.between(localDateTimeStart, endOfStartMonth) + 1; // 含当天
                BigDecimal part = payment.multiply(BigDecimal.valueOf(beforeDays)).setScale(2, RoundingMode.HALF_UP)
                        .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                sum = sum.add(part);
            }
            // 下个月月初
            localDateTimeStart = localDateTimeStart.plusMonths(1).withDayOfMonth(1);
            // 计算 nextStart 到 end 的整月和剩余天数
            int j = 1;
            while (true) {
                LocalDateTime tempEnd = localDateTimeStart.plusMonths(j).minusSeconds(1);
                boolean flag = tempEnd.isAfter(localDateTimeEnd);
                if (!flag) {
                    j += 1;
                } else {
                    j -= 1;
                    //获取到临近的账单开始时间
                    LocalDateTime nextStartTemp = localDateTimeStart.plusMonths(j);
                    if (nextStartTemp.isBefore(localDateTimeEnd)) {
                        long daysDiffs = ChronoUnit.DAYS.between(nextStartTemp, localDateTimeEnd) + 1;
                        BigDecimal tempPart = new BigDecimal(0);
                        tempPart = payment.multiply(new BigDecimal(daysDiffs)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
                        sum = sum.add(tempPart);
                    }
                    //中间的自然月数量
                    BigDecimal fullMonthPayment = payment.multiply(new BigDecimal(j));
                    sum = sum.add(fullMonthPayment);
                    break;
                }
            }
            return sum;
        } else {
            int j = 1;
            while (true) {
                LocalDateTime tempEnd = localDateTimeStart.plusMonths(j).minusSeconds(1);
                boolean flag = tempEnd.isAfter(localDateTimeEnd);
                if (!flag) {
                    j += 1;
                } else {
                    j -= 1;
                    //获取到临近的账单开始时间
                    LocalDateTime nextStartTemp = localDateTimeStart.plusMonths(j);
//                    Date date1 = DateUtils.toDate(nextStartTemp);
//                    Date date2 = DateUtils.toDate(localDateTimeEnd);
//                    if (date1.getTime() < date2.getTime()) {
//                        Long daysDiffs = DateUtils.getDaysDiff(nextStartTemp, localDateTimeEnd) + 1L;
//                        BigDecimal tempPart = new BigDecimal(0);
//                        tempPart = payment.multiply(new BigDecimal(daysDiffs)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
//                        sum = sum.add(tempPart);
//                    }

                    if (nextStartTemp.isBefore(localDateTimeEnd)) {
                        long daysDiffs = ChronoUnit.DAYS.between(nextStartTemp, localDateTimeEnd) + 1;
                        BigDecimal tempPart = new BigDecimal(0);
                        tempPart = payment.multiply(new BigDecimal(daysDiffs)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
                        sum = sum.add(tempPart);
                    }
                    //中间的自然月数量
                    BigDecimal fullMonthPayment = payment.multiply(new BigDecimal(j));
                    sum = sum.add(fullMonthPayment);
                    break;
                }
            }
            return sum;
        }
    }

    /**
     * @param args
     */


    // -------------------- 测试 --------------------
    public static void main(String[] args) {
        BigDecimal payment = new BigDecimal("86800");
        LocalDateTime start = LocalDateTime.of(2024, 11, 2, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 1, 23, 59, 59);
        BigDecimal result = getBigDecimalPlus(BigDecimal.ZERO, start, end, payment);
        System.out.println("测试 (2024-11-16 → 2024-11-29): " + result);
    }

}


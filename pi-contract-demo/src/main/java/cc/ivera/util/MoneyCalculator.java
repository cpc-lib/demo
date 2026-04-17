package cc.ivera.util;

import cc.ivera.domain.bo.ContractPeriodBO;
import cc.ivera.domain.bo.TimeNodeBO;
import cc.ivera.domain.vo.ContractPeriodVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MoneyCalculator {

    public static final int MIN_PAYMENT_MONTHS = 1;
    public static final int MAX_PAYMENT_MONTHS = 12;

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal THIRTY = new BigDecimal("30");

    /**
     * 根据付款周期（月）生成账单账期。
     * 例如：gap=2 表示 2 个月付一次，生成 2 个月一段的账期。
     */
    public List<ContractPeriodVO> contractPeriods(Date startDate, Date stopDate, Integer gap) {
        validateContractRange(startDate, stopDate, gap);
        validatePaymentMonths(gap);

        LocalDateTime contractStartDateTime = DateUtils.convertDateToLocalDateTime(startDate);
        LocalDateTime contractEndDateTime = DateUtils.convertDateToLocalDateTime(stopDate);

        List<ContractPeriodVO> result = new ArrayList<>();
        LocalDateTime periodStartDateTime = contractStartDateTime;
        int number = 1;
        while (!periodStartDateTime.isAfter(contractEndDateTime)) {
            LocalDateTime candidateEnd = periodStartDateTime.plusMonths(gap).minusSeconds(1);
            LocalDateTime actualEnd = candidateEnd.isBefore(contractEndDateTime) ? candidateEnd : contractEndDateTime;
            result.add(buildContractPeriod(number++, periodStartDateTime, actualEnd));
            if (!candidateEnd.isBefore(contractEndDateTime)) {
                break;
            }
            periodStartDateTime = actualEnd.plusSeconds(1);
        }
        return result;
    }

    public ContractPeriodVO calculate(LocalDateTime periodStartDateTime, LocalDateTime periodEndDateTime) {
        if (periodStartDateTime == null || periodEndDateTime == null) {
            throw new IllegalArgumentException("账期开始时间和结束时间不能为空");
        }
        if (periodStartDateTime.isAfter(periodEndDateTime)) {
            throw new IllegalArgumentException("账期开始时间不能晚于结束时间");
        }
        return buildContractPeriod(null, periodStartDateTime, periodEndDateTime);
    }

    /**
     * 统一入口：按账单周期类型计算金额。
     */
    public static List<ContractPeriodVO> calculateByCycleType(List<ContractPeriodBO> stagePeriods,
                                                              List<ContractPeriodVO> billingPeriods,
                                                              BillingCycleType cycleType) {
        Objects.requireNonNull(cycleType, "cycleType 不能为空");
        int paymentMonths = cycleType.getMonths();
        return calculateByPaymentMonths(stagePeriods, billingPeriods, paymentMonths);
    }

    /**
     * 统一入口：直接根据合同起止时间与账单周期类型生成账期并计算金额。
     */
    public static List<ContractPeriodVO> calculateByCycleType(List<ContractPeriodBO> stagePeriods,
                                                              Date contractStart,
                                                              Date contractEnd,
                                                              BillingCycleType cycleType) {
        Objects.requireNonNull(cycleType, "cycleType 不能为空");
        MoneyCalculator calculator = new MoneyCalculator();
        List<ContractPeriodVO> billingPeriods = calculator.contractPeriods(contractStart, contractEnd, cycleType.getMonths());
        return calculateByCycleType(stagePeriods, billingPeriods, cycleType);
    }

    private static List<ContractPeriodVO> calculateByPaymentMonths(List<ContractPeriodBO> stagePeriods,
                                                                   List<ContractPeriodVO> billingPeriods,
                                                                   int paymentMonths) {
        validatePaymentMonths(paymentMonths);
        return calculatePeriods(stagePeriods, billingPeriods, resolveChargeMode(paymentMonths));
    }

    private static ChargeMode resolveChargeMode(int paymentMonths) {
        return paymentMonths == 1 ? ChargeMode.START_ANCHORED_MONTH : ChargeMode.NATURAL_MONTH_WITH_PARTIAL_HEAD;
    }

    private static List<ContractPeriodVO> calculatePeriods(List<ContractPeriodBO> stagePeriods,
                                                           List<ContractPeriodVO> billingPeriods,
                                                           ChargeMode chargeMode) {
        if (billingPeriods == null || billingPeriods.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContractPeriodVO> result = new ArrayList<>(billingPeriods.size());
        List<ContractPeriodBO> safeStagePeriods = stagePeriods == null ? Collections.emptyList() : stagePeriods;

        for (ContractPeriodVO billingPeriod : billingPeriods) {
            if (billingPeriod == null) {
                continue;
            }
            Date periodStart = billingPeriod.getPeriodStart();
            Date periodEnd = billingPeriod.getPeriodEnd();
            validatePeriod(periodStart, periodEnd, "账单账期");

            BigDecimal total = ZERO;
            for (ContractPeriodBO stage : safeStagePeriods) {
                if (stage == null) {
                    continue;
                }
                Date stageStart = stage.getStartTime();
                Date stageEnd = stage.getEndTime();
                validatePeriod(stageStart, stageEnd, "合同阶段");

                if (!hasOverlap(periodStart, periodEnd, stageStart, stageEnd)) {
                    continue;
                }

                Date overlapStart = max(periodStart, stageStart);
                Date overlapEnd = min(periodEnd, stageEnd);
                total = total.add(calculateOverlapAmount(overlapStart, overlapEnd, stage.getPayment(), chargeMode));
            }

            ContractPeriodVO target = copyPeriod(billingPeriod);
            target.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, periodStart));
            target.setMonthRent(total.setScale(2, RoundingMode.HALF_UP));
            result.add(target);
        }
        return result;
    }

    private static BigDecimal calculateOverlapAmount(Date overlapStart,
                                                     Date overlapEnd,
                                                     BigDecimal payment,
                                                     ChargeMode chargeMode) {
        if (payment == null) {
            return ZERO;
        }
        if (chargeMode == ChargeMode.START_ANCHORED_MONTH) {
            return calculateStartAnchoredAmount(LocalDateTimeRange.of(overlapStart, overlapEnd), payment);
        }

        if (DateUtils.isFirstDayOfMonth(overlapStart)) {
            return calculateStartAnchoredAmount(LocalDateTimeRange.of(overlapStart, overlapEnd), payment);
        }

        TimeNodeBO timeNodeBO = new TimeNodeBO();
        timeNodeBO.setNodeStartTime(overlapStart);
        timeNodeBO.setNodeEndTime(overlapEnd);
        return calculateNaturalMonthAmount(timeNodeBO, payment);
    }

    private static BigDecimal calculateNaturalMonthAmount(TimeNodeBO timeNodeBO, BigDecimal payment) {
        BigDecimal sum = ZERO;
        Date nodeStart = timeNodeBO.getNodeStartTime();
        Date monthEnd = DateUtils.getEnd(nodeStart);
        Date nodeEnd = timeNodeBO.getNodeEndTime();
        LocalDateTime periodEndDateTime = DateUtils.convertDateToLocalDateTime(nodeEnd);

        if (!nodeEnd.after(monthEnd)) {
            long daysDiff = DateUtils.getDaysDiff(nodeStart, nodeEnd);
            return sum.add(prorateByThirtyDays(payment, daysDiff));
        }

        long firstPartialDays = DateUtils.getDaysDiff(nodeStart, monthEnd);
        sum = sum.add(prorateByThirtyDays(payment, firstPartialDays));

        LocalDateTime nextWholeDateTimeStart = DateUtils.convertDateToLocalDateTime(monthEnd).plusSeconds(1);
        int fullMonths = 1;
        while (true) {
            LocalDateTime tempEnd = nextWholeDateTimeStart.plusMonths(fullMonths).minusSeconds(1);
            if (!tempEnd.isAfter(periodEndDateTime)) {
                fullMonths++;
                continue;
            }

            fullMonths--;
            LocalDateTime nextStart = nextWholeDateTimeStart.plusMonths(fullMonths);
            Date nextStartDate = DateUtils.toDate(nextStart);
            Date endDate = DateUtils.toDate(periodEndDateTime);
            if (nextStartDate.before(endDate)) {
                long tailDays = DateUtils.getDaysDiff(nextStart, periodEndDateTime) + 1L;
                sum = sum.add(prorateByThirtyDays(payment, tailDays));
            }
            sum = sum.add(payment.multiply(BigDecimal.valueOf(fullMonths)));
            return sum;
        }
    }

    private static BigDecimal calculateStartAnchoredAmount(LocalDateTimeRange range, BigDecimal payment) {
        BigDecimal sum = ZERO;
        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        int fullMonths = 1;
        while (true) {
            LocalDateTime tempEnd = start.plusMonths(fullMonths).minusSeconds(1);
            if (!tempEnd.isAfter(end)) {
                fullMonths++;
                continue;
            }

            fullMonths--;
            LocalDateTime nextStart = start.plusMonths(fullMonths);
            Date nextStartDate = DateUtils.toDate(nextStart);
            Date endDate = DateUtils.toDate(end);
            if (nextStartDate.before(endDate)) {
                long tailDays = DateUtils.getDaysDiff(nextStart, end) + 1L;
                sum = sum.add(prorateByThirtyDays(payment, tailDays));
            }
            sum = sum.add(payment.multiply(BigDecimal.valueOf(fullMonths)));
            return sum;
        }
    }

    private static BigDecimal prorateByThirtyDays(BigDecimal payment, long days) {
        return payment.multiply(BigDecimal.valueOf(days)).divide(THIRTY, 2, RoundingMode.HALF_UP);
    }

    private static boolean hasOverlap(Date periodStart, Date periodEnd, Date stageStart, Date stageEnd) {
        return !stageEnd.before(periodStart) && !stageStart.after(periodEnd);
    }

    private static Date max(Date left, Date right) {
        return left.after(right) ? left : right;
    }

    private static Date min(Date left, Date right) {
        return left.before(right) ? left : right;
    }

    private static void validateContractRange(Date startDate, Date stopDate, Integer gap) {
        if (startDate == null || stopDate == null) {
            throw new IllegalArgumentException("合同开始时间和结束时间不能为空");
        }
        if (gap == null || gap <= 0) {
            throw new IllegalArgumentException("gap 必须大于 0");
        }
        if (startDate.after(stopDate)) {
            throw new IllegalArgumentException("合同开始时间不能晚于结束时间");
        }
    }

    private static void validatePaymentMonths(Integer paymentMonths) {
        if (paymentMonths == null || paymentMonths < MIN_PAYMENT_MONTHS || paymentMonths > MAX_PAYMENT_MONTHS) {
            throw new IllegalArgumentException("付款周期仅支持 1~12 个月，当前值: " + paymentMonths);
        }
    }

    private static void validatePeriod(Date start, Date end, String label) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(label + "开始时间和结束时间不能为空");
        }
        if (start.after(end)) {
            throw new IllegalArgumentException(label + "开始时间不能晚于结束时间");
        }
    }

    private static ContractPeriodVO buildContractPeriod(Integer number,
                                                        LocalDateTime periodStartDateTime,
                                                        LocalDateTime periodEndDateTime) {
        ContractPeriodVO contractPeriodVO = new ContractPeriodVO();
        contractPeriodVO.setNumber(number);
        contractPeriodVO.setPeriodStart(DateUtils.toDate(periodStartDateTime));
        contractPeriodVO.setPeriodEnd(DateUtils.toDate(periodEndDateTime));
        contractPeriodVO.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, DateUtils.toDate(periodStartDateTime)));
        return contractPeriodVO;
    }

    private static ContractPeriodVO copyPeriod(ContractPeriodVO source) {
        ContractPeriodVO target = new ContractPeriodVO();
        target.setNumber(source.getNumber());
        target.setPeriodStart(source.getPeriodStart());
        target.setPeriodEnd(source.getPeriodEnd());
        target.setYear(source.getYear());
        target.setMonthRent(source.getMonthRent());
        target.setNormalFlag(source.getNormalFlag());
        target.setYearMonth(source.getYearMonth());
        return target;
    }

    public enum BillingCycleType {
        ONE(1),
        THREE(3),
        SIX(6),
        TWELVE(12);

        private final int months;

        BillingCycleType(int months) {
            this.months = months;
        }

        public int getMonths() {
            return months;
        }
    }

    private enum ChargeMode {
        START_ANCHORED_MONTH,
        NATURAL_MONTH_WITH_PARTIAL_HEAD
    }

    private record LocalDateTimeRange(LocalDateTime start, LocalDateTime end) {
        private static LocalDateTimeRange of(Date start, Date end) {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            return new LocalDateTimeRange(DateUtils.convertDateToLocalDateTime(start), DateUtils.convertDateToLocalDateTime(end));
        }
    }
}

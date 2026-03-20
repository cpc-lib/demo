package cc.ivera.test.biz;


import cc.ivera.model.bo.ContractPeriodBo;
import cc.ivera.model.vo.ContractPeriodVo;
import cc.ivera.util.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MoneyCalculator {


    public List<ContractPeriodVo> contractPeriods(Date startDate, Date stopDate, Integer gap) {
        // 合同开始时间
        LocalDateTime contractStartDateTime = DateUtils.convertDateToLocalDateTime(startDate);
        // 合同结束时间
        LocalDateTime contractEndDateTime = DateUtils.convertDateToLocalDateTime(stopDate);
        LocalDateTime periodStartDateTime = contractStartDateTime;
        LocalDateTime periodEndDateTime = periodStartDateTime.plusMonths(gap).minusSeconds(1);
        List<ContractPeriodVo> contractPeriodVos = new ArrayList<>();
        while (periodEndDateTime.isBefore(contractEndDateTime)) {
            ContractPeriodVo contractPeriodVo = calculate(periodStartDateTime, periodEndDateTime);
            contractPeriodVos.add(contractPeriodVo);
            periodStartDateTime = periodEndDateTime.plusSeconds(1);
            periodEndDateTime = periodStartDateTime.plusMonths(gap).minusSeconds(1);
        }
        ContractPeriodVo contractPeriodVo = calculate(periodStartDateTime, contractEndDateTime);
        contractPeriodVos.add(contractPeriodVo);
        return contractPeriodVos;
    }


    public ContractPeriodVo calculate(LocalDateTime periodStartDateTime, LocalDateTime periodEndDateTime) {
        ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
        contractPeriodVo.setPeriodStart(DateUtils.toDate(periodStartDateTime));
        contractPeriodVo.setPeriodEnd(DateUtils.toDate(periodEndDateTime));
        String yearMonth = DateUtils.parseDateToStr(DateUtils.YYYY_MM, DateUtils.toDate(periodStartDateTime));
        contractPeriodVo.setYearMonth(yearMonth);
        return contractPeriodVo;
    }


    public static List<ContractPeriodVo> doMonthCalculate(List<ContractPeriodBo> sortedVos, List<ContractPeriodVo> contractPeriodVos) {
        //获取到一个账单时间信息
        List<ContractPeriodVo> vos = new ArrayList<>();
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            Date p0 = contractPeriodVo.getPeriodStart();
            Date p1 = contractPeriodVo.getPeriodEnd();
            //阶段收费信息
            BigDecimal sum = new BigDecimal("0.00");
            for (ContractPeriodBo sortedVo : sortedVos) {
                //判断每个阶段是否都在计算范围内
                Date a0 = sortedVo.getStartTime();
                Date a1 = sortedVo.getEndTime();
                BigDecimal value = new BigDecimal("0.00");
                if (a1.getTime() <= p0.getTime() || a0.getTime() >= p1.getTime()) {

                } else {
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p0.getTime() && a1.getTime() <= p1.getTime()) {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);
                    }
                    if (a1.getTime() > p1.getTime() && a0.getTime() < p1.getTime() && a0.getTime() >= p0.getTime()) {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);
                    }
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p1.getTime()) {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);
                    }
                    if (a0.getTime() == p0.getTime() && a1.getTime() == p1.getTime()) {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);
                    }
                    if (a0.getTime() > p0.getTime() && a0.getTime() < p1.getTime() && a1.getTime() > p0.getTime() && a1.getTime() < p1.getTime()) {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);
                    }
                }
            }
            //年月
            contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, p0));
            contractPeriodVo.setMonthRent(sum);
            vos.add(contractPeriodVo);
        }
        return vos;
    }


    public static List<ContractPeriodVo> doSeasonCalculate(List<ContractPeriodBo> sortedVos, List<ContractPeriodVo> contractPeriodVos) {
        //获取到一个账单时间信息
        List<ContractPeriodVo> vos = new ArrayList<>();
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            Date p0 = contractPeriodVo.getPeriodStart();
            Date p1 = contractPeriodVo.getPeriodEnd();
            //阶段收费信息
            BigDecimal sum = new BigDecimal("0.00");
            for (ContractPeriodBo sortedVo : sortedVos) {
                //判断每个阶段是否都在计算范围内
                Date a0 = sortedVo.getStartTime();
                Date a1 = sortedVo.getEndTime();
                BigDecimal value = new BigDecimal("0.00");
                if (a1.getTime() <= p0.getTime() || a0.getTime() >= p1.getTime()) {

                } else {
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p0.getTime() && a1.getTime() <= p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a1.getTime() > p1.getTime() && a0.getTime() < p1.getTime() && a0.getTime() >= p0.getTime()) {
                        //计算有几个自然月
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);

                    }
                    if (a0.getTime() == p0.getTime() && a1.getTime() == p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a0.getTime() > p0.getTime() && a0.getTime() < p1.getTime() && a1.getTime() > p0.getTime() && a1.getTime() < p1.getTime()) {
                        //a0a1
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                }
            }
            //年月
            contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, p0));
            contractPeriodVo.setMonthRent(sum);
            vos.add(contractPeriodVo);
        }
        return vos;
    }


    public static List<ContractPeriodVo> doHalfYearCalculate(List<ContractPeriodBo> sortedVos, List<ContractPeriodVo> contractPeriodVos) {
        //获取到一个账单时间信息
        List<ContractPeriodVo> vos = new ArrayList<>();
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            Date p0 = contractPeriodVo.getPeriodStart();
            Date p1 = contractPeriodVo.getPeriodEnd();
            //阶段收费信息
            BigDecimal sum = new BigDecimal("0.00");
            for (ContractPeriodBo sortedVo : sortedVos) {
                //判断每个阶段是否都在计算范围内
                Date a0 = sortedVo.getStartTime();
                Date a1 = sortedVo.getEndTime();
                BigDecimal value = new BigDecimal("0.00");
                if (a1.getTime() <= p0.getTime() || a0.getTime() >= p1.getTime()) {

                } else {
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p0.getTime() && a1.getTime() <= p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a1.getTime() > p1.getTime() && a0.getTime() < p1.getTime() && a0.getTime() >= p0.getTime()) {
                        //计算有几个自然月
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);

                    }
                    if (a0.getTime() == p0.getTime() && a1.getTime() == p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                    if (a0.getTime() > p0.getTime() && a0.getTime() < p1.getTime() && a1.getTime() > p0.getTime() && a1.getTime() < p1.getTime()) {
                        //a0a1
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    }
                }
            }
            //年月
            contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, p0));
            contractPeriodVo.setMonthRent(sum);
            vos.add(contractPeriodVo);
        }
        return vos;
    }


    public static List<ContractPeriodVo> doYearCalculate(List<ContractPeriodBo> sortedVos, List<ContractPeriodVo> contractPeriodVos) {
        //获取到一个账单时间信息
        List<ContractPeriodVo> vos = new ArrayList<>();
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            Date p0 = contractPeriodVo.getPeriodStart();
            Date p1 = contractPeriodVo.getPeriodEnd();
            //阶段收费信息
            BigDecimal sum = new BigDecimal("0.00");
            for (ContractPeriodBo sortedVo : sortedVos) {
                //判断每个阶段是否都在计算范围内
                Date a0 = sortedVo.getStartTime();
                Date a1 = sortedVo.getEndTime();
                BigDecimal value = new BigDecimal("0.00");
                if (a1.getTime() <= p0.getTime() || a0.getTime() >= p1.getTime()) {

                } else {
                    if (a0.getTime() < p0.getTime() && a1.getTime() > p0.getTime() && a1.getTime() <= p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    } else if (a1.getTime() > p1.getTime() && a0.getTime() < p1.getTime() && a0.getTime() >= p0.getTime()) {
                        //计算有几个自然月
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    } else if (a0.getTime() < p0.getTime() && a1.getTime() > p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);

                    } else if (a0.getTime() == p0.getTime() && a1.getTime() == p1.getTime()) {
                        if (DateUtils.isFirstDayOfMonth(p0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(p0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(p0);
                            timeNode.setNodeEndTime(p1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    } else if (a0.getTime() > p0.getTime() && a0.getTime() < p1.getTime() && a1.getTime() > p0.getTime() && a1.getTime() < p1.getTime()) {
                        //a0a1
                        if (DateUtils.isFirstDayOfMonth(a0)) {
                            LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                            LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        } else {
                            TimeNode timeNode = new TimeNode();
                            timeNode.setNodeStartTime(a0);
                            timeNode.setNodeEndTime(a1);
                            value = getBigDecimal(new BigDecimal("0.00"), timeNode, sortedVo.getPayment());
                        }
                        sum = sum.add(value);
                    } else {
                        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(a0);
                        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(a1);
                        value = getBigDecimal(new BigDecimal("0.00"), localDateTimeStart, localDateTimeEnd, sortedVo.getPayment());
                        sum = sum.add(value);

                    }
                }
            }
            //年月
            contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, p0));
            contractPeriodVo.setMonthRent(sum);
            vos.add(contractPeriodVo);
        }
        return vos;
    }


    private static BigDecimal getBigDecimal(BigDecimal sum, TimeNode timeNode, BigDecimal payment) {
        Date nodeStart = timeNode.getNodeStartTime();
        //获取到本月的结尾
        Date monthEnd = DateUtils.getEnd(timeNode.getNodeStartTime());
        Date nodeEnd = timeNode.getNodeEndTime();
        LocalDateTime periodEndDateTime = DateUtils.convertDateToLocalDateTime(nodeEnd);
        if (nodeEnd.getTime() <= monthEnd.getTime()) {
            Long daysDiff = DateUtils.getDaysDiff(nodeStart, nodeEnd);
            BigDecimal monthPayment = payment;
            BigDecimal part = monthPayment.multiply(new BigDecimal(daysDiff)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
            sum = sum.add(part);
        } else {
            //获取到本月截止时间的金额
            Long daysDiff = DateUtils.getDaysDiff(nodeStart, monthEnd);
            BigDecimal monthPayment = payment;
            BigDecimal part = new BigDecimal(0);
            part = monthPayment.multiply(new BigDecimal(daysDiff)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
            sum = sum.add(part);

            LocalDateTime localDateTime = DateUtils.convertDateToLocalDateTime(monthEnd);
            LocalDateTime nextWholeDateTimeStart = localDateTime.plusSeconds(1);
            int j = 1;
            while (true) {
                LocalDateTime tempEnd = nextWholeDateTimeStart.plusMonths(j).minusSeconds(1);
                boolean flag = tempEnd.isAfter(periodEndDateTime);
                if (!flag) {
                    j += 1;
                } else {
                    j -= 1;
                    //获取到临近的账单开始时间
                    LocalDateTime nextStart = nextWholeDateTimeStart.plusMonths(j);
                    Date date1 = DateUtils.toDate(nextStart);
                    Date date2 = DateUtils.toDate(periodEndDateTime);
                    if (date1.getTime() < date2.getTime()) {
                        Long daysDiffs = DateUtils.getDaysDiff(nextStart, periodEndDateTime) + 1L;
                        part = new BigDecimal(0);
                        part = monthPayment.multiply(new BigDecimal(daysDiffs)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
                        sum = sum.add(part);
                    }
                    //中间的自然月数量
                    BigDecimal fullMonthPayment = monthPayment.multiply(new BigDecimal(j));
                    sum = sum.add(fullMonthPayment);
                    break;
                }
            }
        }
        return sum;
    }


    private static BigDecimal getBigDecimal(BigDecimal sum, LocalDateTime localDateTimeStart, LocalDateTime localDateTimeEnd, BigDecimal payment) {
        int j = 1;
        while (true) {
            LocalDateTime tempEnd = localDateTimeStart.plusMonths(j).minusSeconds(1);
            boolean flag = tempEnd.isAfter(localDateTimeEnd);
            if (!flag) {
                j += 1;
            } else {
                j -= 1;
                BigDecimal monthPayment = payment;
                //获取到临近的账单开始时间
                LocalDateTime nextStart = localDateTimeStart.plusMonths(j);
                Date date1 = DateUtils.toDate(nextStart);
                Date date2 = DateUtils.toDate(localDateTimeEnd);
                if (date1.getTime() < date2.getTime()) {
                    Long daysDiffs = DateUtils.getDaysDiff(nextStart, localDateTimeEnd) + 1L;
                    BigDecimal part = new BigDecimal(0);
                    part = monthPayment.multiply(new BigDecimal(daysDiffs)).divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
                    sum = sum.add(part);
                }
                //中间的自然月数量
                BigDecimal fullMonthPayment = monthPayment.multiply(new BigDecimal(j));
                sum = sum.add(fullMonthPayment);
                break;
            }
        }
        return sum;
    }


    private static BigDecimal getBigDecimalV1(BigDecimal sum, LocalDateTime localDateTimeStart, LocalDateTime localDateTimeEnd, BigDecimal payment) {
        // 判断是否刚好是 n 个自然月
        LocalDateTime temp = localDateTimeStart;
        int months = 0;
        while (temp.isBefore(localDateTimeEnd) || temp.equals(localDateTimeEnd)) {
            if (temp.equals(localDateTimeEnd)) {
                return payment.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
            }
            temp = temp.plusMonths(1).minusSeconds(1);
            months++;
        }

        // 不是整月：判断是否是月初
        if (localDateTimeStart.getDayOfMonth() != 1) {
            // 计算从开始日期到当月月底的天数
            LocalDateTime endOfStartMonth = localDateTimeStart.toLocalDate().plusMonths(1).withDayOfMonth(1).minusDays(1).atStartOfDay();
            long beforeDays = ChronoUnit.DAYS.between(localDateTimeStart.toLocalDate(), endOfStartMonth.toLocalDate()) + 1; // 含当天
            BigDecimal part = payment.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(beforeDays));

            // 下个月月初
            LocalDateTime nextStart = localDateTimeStart.toLocalDate().plusMonths(1).withDayOfMonth(1).atStartOfDay();
            // 计算 nextStart 到 end 的整月和剩余天数
            int m = 0;
            LocalDateTime tempEnd = nextStart;
            while (tempEnd.toLocalDate().plusMonths(1).isBefore(localDateTimeEnd.toLocalDate()) ||
                    tempEnd.toLocalDate().plusMonths(1).equals(localDateTimeEnd.toLocalDate())) {
                tempEnd = tempEnd.plusMonths(1);
                m++;
            }
            long leftDays = ChronoUnit.DAYS.between(tempEnd.toLocalDate(), localDateTimeEnd.toLocalDate()) + 1;

            return part.add(payment.multiply(BigDecimal.valueOf(m)))
                    .add(payment.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(leftDays)))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            // 是月初：计算整月数 m 和剩余天数 n
            LocalDateTime tempDate = localDateTimeStart;
            int m = 0;
            while (tempDate.toLocalDate().plusMonths(1).isBefore(localDateTimeEnd.toLocalDate()) ||
                    tempDate.toLocalDate().plusMonths(1).equals(localDateTimeEnd.toLocalDate())) {
                tempDate = tempDate.plusMonths(1);
                m++;
            }
            long n = ChronoUnit.DAYS.between(tempDate.toLocalDate(), localDateTimeEnd.toLocalDate()) + 1;
            return payment.multiply(BigDecimal.valueOf(m))
                    .add(payment.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(n)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }


}

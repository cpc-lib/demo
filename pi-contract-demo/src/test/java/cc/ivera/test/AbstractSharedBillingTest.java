package cc.ivera.test;

import cc.ivera.domain.bo.ContractPeriodBO;
import cc.ivera.domain.vo.ContractPeriodVO;
import cc.ivera.util.DateUtils;
import cc.ivera.util.PeriodCalculator;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractSharedBillingTest {

    protected static final String CONTRACT_MANAGE_ID = "1001";
    protected static final Date QUERY_START = parse("2024-04-01 00:00:00");
    protected static final Date QUERY_END = parse("2025-02-02 23:59:59");

    protected static Date parse(String value) {
        try {
            return DateUtils.parseDate(value, DateUtils.YYYY_MM_DD_HH_MM_SS);
        } catch (ParseException e) {
            throw new IllegalArgumentException("日期解析失败: " + value, e);
        }
    }

    /**
     * 所有测试统一使用这一组合同阶段数据
     */
    protected List<ContractPeriodBO> buildSharedStagePeriods() {
        List<ContractPeriodBO> periods = new ArrayList<>();
        periods.add(stage("1002", "2015-05-16 00:00:00", "2016-05-15 23:59:59", "0", "0"));
        periods.add(stage("1008", "2016-05-16 00:00:00", "2016-06-30 23:59:59", "60000", "1"));
        periods.add(stage("1003", "2016-07-01 00:00:00", "2018-05-15 23:59:59", "60000", "0"));
        periods.add(stage("1004", "2018-05-16 00:00:00", "2020-05-15 23:59:59", "66000", "0"));
        periods.add(stage("1005", "2020-05-16 00:00:00", "2022-05-15 23:59:59", "72600", "0"));
        periods.add(stage("1006", "2022-05-16 00:00:00", "2024-05-15 23:59:59", "79860", "0"));
        periods.add(stage("1007", "2024-05-16 00:00:00", "2025-05-15 23:59:59", "87846", "0"));
        return PeriodCalculator.getContractPeriodVos(periods);
    }

    /**
     * 所有测试统一使用这一组查询区间，只是 gap 不同
     */
    protected List<ContractPeriodVO> buildBillingPeriods(int gapMonths) {
        return PeriodCalculator.getContractPeriod(QUERY_START, QUERY_END, gapMonths);
    }

    protected void assertContractPeriod(ContractPeriodVO actual,
                                        int expectedNumber,
                                        String expectedStart,
                                        String expectedEnd,
                                        String expectedRent) {
        assertNotNull(actual);
        assertEquals(Integer.valueOf(expectedNumber), actual.getNumber());
        assertEquals(expectedStart, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, actual.getPeriodStart()));
        assertEquals(expectedEnd, DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, actual.getPeriodEnd()));
        assertBigDecimalEquals(expectedRent, actual.getMonthRent());
    }

    protected void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    protected BigDecimal sumRent(List<ContractPeriodVO> periods) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractPeriodVO period : periods) {
            if (period != null && period.getMonthRent() != null) {
                total = total.add(period.getMonthRent());
            }
        }
        return total;
    }

    private ContractPeriodBO stage(String periodId,
                                   String start,
                                   String end,
                                   String payment,
                                   String wholeFlag) {
        ContractPeriodBO period = new ContractPeriodBO();
        period.setContractManageId(CONTRACT_MANAGE_ID);
        period.setContractPeriodId(periodId);
        period.setStartTime(parse(start));
        period.setEndTime(parse(end));
        period.setPayment(new BigDecimal(payment));
        period.setWholeFlag(wholeFlag);
        return period;
    }
}
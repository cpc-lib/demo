package cc.ivera.test;

import cc.ivera.domain.vo.ContractPeriodVO;
import cc.ivera.util.MoneyCalculator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MonthTest extends AbstractSharedBillingTest {

    @Test
    public void shouldCalculateMonthPeriodsWithSharedData() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                buildBillingPeriods(1),
                MoneyCalculator.BillingCycleType.ONE
        );

        assertEquals(11, result.size());
        assertContractPeriod(result.get(0), 1, "2024-04-01 00:00:00", "2024-04-30 23:59:59", "79860.00");
        assertContractPeriod(result.get(1), 2, "2024-05-01 00:00:00", "2024-05-31 23:59:59", "86781.20");
        assertContractPeriod(result.get(10), 11, "2025-02-01 00:00:00", "2025-02-02 23:59:59", "5856.40");
        assertBigDecimalEquals("875265.60", sumRent(result));
    }

    @Test
    public void shouldSupportDateRangeOverloadForMonthCycle() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                QUERY_START,
                QUERY_END,
                MoneyCalculator.BillingCycleType.ONE
        );

        assertEquals(11, result.size());
        assertBigDecimalEquals("875265.60", sumRent(result));
    }
}
package cc.ivera.test;

import cc.ivera.domain.vo.ContractPeriodVO;
import cc.ivera.util.MoneyCalculator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class HalfYearTest extends AbstractSharedBillingTest {

    @Test
    public void shouldCalculateHalfYearPeriodsWithSharedData() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                buildBillingPeriods(6),
                MoneyCalculator.BillingCycleType.SIX
        );

        assertEquals(2, result.size());
        assertContractPeriod(result.get(0), 1, "2024-04-01 00:00:00", "2024-09-30 23:59:59", "518025.20");
        assertContractPeriod(result.get(1), 2, "2024-10-01 00:00:00", "2025-02-02 23:59:59", "357240.40");
        assertBigDecimalEquals("875265.60", sumRent(result));
    }

    @Test
    public void shouldSupportDateRangeOverloadForHalfYearCycle() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                QUERY_START,
                QUERY_END,
                MoneyCalculator.BillingCycleType.SIX
        );

        assertEquals(2, result.size());
        assertBigDecimalEquals("875265.60", sumRent(result));
    }
}
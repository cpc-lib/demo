package cc.ivera.test;

import cc.ivera.domain.vo.ContractPeriodVO;
import cc.ivera.util.MoneyCalculator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class YearTest extends AbstractSharedBillingTest {

    @Test
    public void shouldCalculateYearPeriodsWithSharedData() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                buildBillingPeriods(12),
                MoneyCalculator.BillingCycleType.TWELVE
        );

        assertEquals(1, result.size());
        assertContractPeriod(result.get(0), 1, "2024-04-01 00:00:00", "2025-02-02 23:59:59", "875265.60");
        assertBigDecimalEquals("875265.60", sumRent(result));
    }

    @Test
    public void shouldSupportDateRangeOverloadForYearCycle() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                QUERY_START,
                QUERY_END,
                MoneyCalculator.BillingCycleType.TWELVE
        );

        assertEquals(1, result.size());
        assertBigDecimalEquals("875265.60", sumRent(result));
    }
}
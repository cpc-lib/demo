package cc.ivera.test;

import cc.ivera.domain.vo.ContractPeriodVO;
import cc.ivera.util.MoneyCalculator;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SeasonTest extends AbstractSharedBillingTest {

    @Test
    public void shouldCalculateSeasonPeriodsWithSharedData() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                buildBillingPeriods(3),
                MoneyCalculator.BillingCycleType.THREE
        );

        assertEquals(4, result.size());
        assertContractPeriod(result.get(0), 1, "2024-04-01 00:00:00", "2024-06-30 23:59:59", "254487.20");
        assertContractPeriod(result.get(1), 2, "2024-07-01 00:00:00", "2024-09-30 23:59:59", "263538.00");
        assertContractPeriod(result.get(3), 4, "2025-01-01 00:00:00", "2025-02-02 23:59:59", "93702.40");
        assertBigDecimalEquals("875265.60", sumRent(result));
    }

    @Test
    public void shouldSupportDateRangeOverloadForSeasonCycle() {
        List<ContractPeriodVO> result = MoneyCalculator.calculateByCycleType(
                buildSharedStagePeriods(),
                QUERY_START,
                QUERY_END,
                MoneyCalculator.BillingCycleType.THREE
        );

        assertEquals(4, result.size());
        assertBigDecimalEquals("875265.60", sumRent(result));
    }
}
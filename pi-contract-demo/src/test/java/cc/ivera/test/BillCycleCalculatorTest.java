package cc.ivera.test;

import cc.ivera.domain.bo.BillCycleBO;
import cc.ivera.domain.vo.BillCycleVO;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cc.ivera.util.BillCycleCalculator.adjustContractPeriod;

public class BillCycleCalculatorTest {


    @Test
    public void testBillCycleCalculator() {
        LocalDateTime contractStart = LocalDateTime.parse("2024-01-02T00:00:00");
        LocalDateTime contractEnd = LocalDateTime.parse("2026-02-16T23:59:59");

        List<BillCycleVO> fixedCycles = new ArrayList<>();
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2024-01-02T00:00:00"), LocalDateTime.parse("2024-02-29T23:59:59")));
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2024-05-02T00:00:00"), LocalDateTime.parse("2024-07-31T23:59:59")));
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2026-01-01T00:00:00"), LocalDateTime.parse("2026-02-16T23:59:59")));

        List<BillCycleBO> billCycleBOs = adjustContractPeriod(contractStart, contractEnd, fixedCycles);
        for (BillCycleBO billCycleBO : billCycleBOs) {
            System.out.println(billCycleBO);
        }
    }
}

package cc.ivera.util;

import cc.ivera.domain.bo.BillCycleBO;
import cc.ivera.domain.vo.BillCycleVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BillCycleCalculator {

    private BillCycleCalculator() {
    }




    public static List<BillCycleBO> adjustContractPeriod(LocalDateTime contractStart,
                                                         LocalDateTime contractEnd,
                                                         List<BillCycleVO> fixedCycles) {
        validateContract(contractStart, contractEnd);
        List<BillCycleVO> normalizedFixedCycles = normalizeFixedCycles(contractStart, contractEnd, fixedCycles);
        List<BillCycleVO> allCycles = calculateBillCycles(contractStart, contractEnd, normalizedFixedCycles);
        Set<String> fixedKeys = normalizedFixedCycles.stream()
                .map(BillCycleCalculator::rangeKey)
                .collect(Collectors.toCollection(HashSet::new));

        List<BillCycleBO> result = new ArrayList<>(allCycles.size());
        for (BillCycleVO cycle : allCycles) {
            BillCycleBO billCycleBO = new BillCycleBO();
            billCycleBO.setStartTime(DateUtils.toDate(cycle.getStartTime()));
            billCycleBO.setEndTime(DateUtils.toDate(cycle.getEndTime()));
            billCycleBO.setWholeFlag(fixedKeys.contains(rangeKey(cycle)) ? "1" : "0");
            result.add(billCycleBO);
        }
        return result;
    }

    public static List<BillCycleVO> calculateBillCycles(LocalDateTime contractStart,
                                                        LocalDateTime contractEnd,
                                                        List<BillCycleVO> fixedCycles) {
        validateContract(contractStart, contractEnd);
        List<BillCycleVO> normalizedFixedCycles = normalizeFixedCycles(contractStart, contractEnd, fixedCycles);

        List<BillCycleVO> allCycles = new ArrayList<>();
        LocalDateTime current = contractStart;
        for (BillCycleVO fixedCycle : normalizedFixedCycles) {
            if (current.isBefore(fixedCycle.getStartTime())) {
                allCycles.add(new BillCycleVO(current, fixedCycle.getStartTime().minusSeconds(1)));
            }
            allCycles.add(fixedCycle);
            current = fixedCycle.getEndTime().plusSeconds(1);
        }

        if (!current.isAfter(contractEnd)) {
            allCycles.add(new BillCycleVO(current, contractEnd));
        }
        return allCycles;
    }

    private static List<BillCycleVO> normalizeFixedCycles(LocalDateTime contractStart,
                                                          LocalDateTime contractEnd,
                                                          List<BillCycleVO> fixedCycles) {
        if (fixedCycles == null || fixedCycles.isEmpty()) {
            return List.of();
        }

        List<BillCycleVO> normalized = fixedCycles.stream()
                .filter(Objects::nonNull)
                .map(cycle -> clampCycle(contractStart, contractEnd, cycle))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BillCycleVO::getStartTime).thenComparing(BillCycleVO::getEndTime))
                .collect(Collectors.toList());

        BillCycleVO previous = null;
        for (BillCycleVO current : normalized) {
            validateCycle(current);
            if (previous != null && !current.getStartTime().isAfter(previous.getEndTime())) {
                throw new IllegalArgumentException("固定账期存在重叠或相接冲突: " + previous + " <> " + current);
            }
            previous = current;
        }
        return normalized;
    }

    private static BillCycleVO clampCycle(LocalDateTime contractStart, LocalDateTime contractEnd, BillCycleVO cycle) {
        validateCycle(cycle);
        if (cycle.getEndTime().isBefore(contractStart) || cycle.getStartTime().isAfter(contractEnd)) {
            return null;
        }
        LocalDateTime start = cycle.getStartTime().isBefore(contractStart) ? contractStart : cycle.getStartTime();
        LocalDateTime end = cycle.getEndTime().isAfter(contractEnd) ? contractEnd : cycle.getEndTime();
        return new BillCycleVO(start, end);
    }

    private static void validateContract(LocalDateTime contractStart, LocalDateTime contractEnd) {
        if (contractStart == null || contractEnd == null) {
            throw new IllegalArgumentException("合同开始时间和结束时间不能为空");
        }
        if (contractStart.isAfter(contractEnd)) {
            throw new IllegalArgumentException("合同开始时间不能晚于结束时间");
        }
    }

    private static void validateCycle(BillCycleVO cycle) {
        if (cycle == null || cycle.getStartTime() == null || cycle.getEndTime() == null) {
            throw new IllegalArgumentException("账期不能为空");
        }
        if (cycle.getStartTime().isAfter(cycle.getEndTime())) {
            throw new IllegalArgumentException("账期开始时间不能晚于结束时间: " + cycle);
        }
    }

    private static String rangeKey(BillCycleVO cycle) {
        return cycle.getStartTime() + "#" + cycle.getEndTime();
    }
}

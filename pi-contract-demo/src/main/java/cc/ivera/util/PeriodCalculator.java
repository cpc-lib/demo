package cc.ivera.util;

import cc.ivera.domain.bo.ContractPeriodBO;
import cc.ivera.domain.vo.ContractPeriodVO;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PeriodCalculator {

    private PeriodCalculator() {
    }

    /**
     * 根据开始时间从旧到新排序；开始时间一致时按结束时间升序。
     */
    public static List<ContractPeriodBO> getContractPeriodVos(List<ContractPeriodBO> vos) {
        if (vos == null || vos.isEmpty()) {
            return Collections.emptyList();
        }
        return vos.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ContractPeriodBO::getStartTime, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(ContractPeriodBO::getEndTime, Comparator.nullsLast(Date::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 生成付款账期，当前业务支持 1~12 个月一付。
     */
    public static List<ContractPeriodVO> getContractPeriod(Date minTime, Date maxTime, int gap) {
        return new MoneyCalculator().contractPeriods(minTime, maxTime, gap);
    }
}

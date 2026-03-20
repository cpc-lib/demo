package cc.ivera.test.biz;


import cc.ivera.model.bo.ContractPeriodBo;
import cc.ivera.model.vo.ContractPeriodVo;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class PeriodCalculator {

    //根据时间从旧到新排序
    public static List<ContractPeriodBo> getContractPeriodVos(List<ContractPeriodBo> vos) {
        List<ContractPeriodBo> sortedVos = vos.stream().sorted(new Comparator<ContractPeriodBo>() {
            @Override
            public int compare(ContractPeriodBo o1, ContractPeriodBo o2) {
                if (o1.getStartTime().getTime() < o2.getStartTime().getTime()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }).collect(Collectors.toList());
        return sortedVos;
    }


    public static List<ContractPeriodVo> getContractPeriod(Date minTime, Date maxTime, int gap) {
        MoneyCalculator moneyCalculator = new MoneyCalculator();
        List<ContractPeriodVo> contractPeriodVos = moneyCalculator.contractPeriods(minTime, maxTime, gap);
        return contractPeriodVos;
    }

}

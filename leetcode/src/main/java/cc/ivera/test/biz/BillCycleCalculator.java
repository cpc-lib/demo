package cc.ivera.test.biz;

import cc.ivera.model.bo.BillCycleBo;
import cc.ivera.util.DateUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillCycleCalculator {

    public static void main(String[] args) {
        //合同开始时间
        LocalDateTime contractStart = LocalDateTime.parse("2024-01-02T00:00:00");
        //合同结束时间
        LocalDateTime contractEnd = LocalDateTime.parse("2026-02-16T23:59:59");

        // 定义固定账单周期  
        List<BillCycleVO> fixedCycles = new ArrayList<>();
        //固定账期的时间阶段
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2024-01-02T00:00:00"), LocalDateTime.parse("2024-02-29T23:59:59")));
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2024-05-02T00:00:00"), LocalDateTime.parse("2024-07-31T23:59:59")));
        fixedCycles.add(new BillCycleVO(LocalDateTime.parse("2026-01-01T00:00:00"), LocalDateTime.parse("2026-02-16T23:59:59")));


        // 可以根据需要添加更多的固定账单
        List<BillCycleBo> billCycleBos = adujustContractPeriod(contractStart, contractEnd, fixedCycles);
        for (BillCycleBo billCycleBo : billCycleBos) {
            System.out.println(billCycleBo);
        }

    }

    public static List<BillCycleBo> adujustContractPeriod(LocalDateTime contractStart, LocalDateTime contractEnd, List<BillCycleVO> fixedCycles) {
        List<BillCycleVO> allCycles = calculateBillCycles(contractStart, contractEnd, fixedCycles);
        List<BillCycleBo> bos = new ArrayList<>();
        for (BillCycleVO cycle : allCycles) {
            boolean collapseFlag = false;
            List<Boolean> collapseList = new ArrayList<>();
            for (BillCycleVO fixedCycle : fixedCycles) {
                //开始时间
                LocalDateTime startTime = cycle.getStartTime();
                //结束时间
                LocalDateTime endTime = cycle.getEndTime();
                //固定收费阶段的开始时间
                LocalDateTime fixedStartTime = fixedCycle.getStartTime();
                LocalDateTime fixedEndTime = fixedCycle.getEndTime();
                Date startTimeDate = DateUtils.toDate(startTime);
                Date endTimeDate = DateUtils.toDate(endTime);
                Date fixedStartTimeDate = DateUtils.toDate(fixedStartTime);
                Date fixedEndTimeDate = DateUtils.toDate(fixedEndTime);
                if (startTimeDate.getTime() == fixedStartTimeDate.getTime() && endTimeDate.getTime() == fixedEndTimeDate.getTime()) {
                    boolean flag = true;
                    collapseList.add(flag);
                } else {
                    boolean flag = false;
                    collapseList.add(flag);
                }
            }
            for (Boolean value : collapseList) {
                if (value) {
                    collapseFlag = true;
                    break;
                }
            }
            if (!collapseFlag) {
                //非固定周期账单
                //System.out.println(cycle);
                BillCycleBo billCycleBo = new BillCycleBo();
                billCycleBo.setStartTime(DateUtils.toDate(cycle.getStartTime()));
                billCycleBo.setEndTime(DateUtils.toDate(cycle.getEndTime()));
                billCycleBo.setWholeFlag("0");
                bos.add(billCycleBo);
            } else {
                //System.out.println("固定" + cycle);
                BillCycleBo billCycleBo = new BillCycleBo();
                billCycleBo.setStartTime(DateUtils.toDate(cycle.getStartTime()));
                billCycleBo.setEndTime(DateUtils.toDate(cycle.getEndTime()));
                billCycleBo.setWholeFlag("1");
                bos.add(billCycleBo);
            }
        }
        return bos;
    }


    public static List<BillCycleVO> calculateBillCycles(LocalDateTime contractStart, LocalDateTime contractEnd, List<BillCycleVO> fixedCycles) {
        List<BillCycleVO> allCycles = new ArrayList<>();
        LocalDateTime current = contractStart;

        for (BillCycleVO fixedCycle : fixedCycles) {
            if (current.isBefore(fixedCycle.startTime)) {
                // 添加固定账单周期之前的普通账单周期  
                LocalDateTime nextCycleEnd = fixedCycle.startTime.minusSeconds(1);
                allCycles.add(new BillCycleVO(current, nextCycleEnd));
            }
            allCycles.add(fixedCycle); // 添加固定账单周期  
            current = fixedCycle.endTime.plusSeconds(1); // 更新当前时间到固定账单周期之后  
        }

        // 添加最后一个固定账单周期之后的普通账单周期  
        if (current.isBefore(contractEnd)) {
            allCycles.add(new BillCycleVO(current, contractEnd));
        }

        return allCycles;
    }

    static class BillCycleVO {
        LocalDateTime startTime;
        LocalDateTime endTime;

        BillCycleVO(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return this.startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "[" + startTime.format(formatter) + " - " + endTime.format(formatter) + "]";
        }
    }
}
package cc.ivera.test.biz;

import org.junit.Test;
import cc.ivera.model.bo.BillCycleBo;
import cc.ivera.model.bo.ContractPeriodBo;
import cc.ivera.model.vo.ContractPeriodVo;
import cc.ivera.util.DateUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MoneyCalculatorTest {
    @Test
    public void testMonthCalculator() throws ParseException {
        ContractPeriodBo contractPeriodBo = new ContractPeriodBo();
        contractPeriodBo.setContractManageId("1001");
        contractPeriodBo.setContractPeriodId("1002");
        Date start = DateUtils.parseDate("2022-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end = DateUtils.parseDate("2023-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo.setStartTime(start);
        contractPeriodBo.setEndTime(end);
        contractPeriodBo.setPayment(new BigDecimal(1100));
        contractPeriodBo.setWholeFlag("1");


        Date contractStartTime = DateUtils.parseDate("2022-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date contractEndTime = DateUtils.parseDate("2023-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);

        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo);

        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);
        //月度 获取所有账单的开始时间与结束时间
        List<ContractPeriodVo> monthContractPeriodVo = PeriodCalculator.getContractPeriod(contractStartTime, contractEndTime, 1);
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doMonthCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }

    @Test
    public void testMonthCalculatorV1() throws ParseException {

        //合同开始时间
        Date contractStartTime = DateUtils.parseDate("2022-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(contractStartTime);
        //合同结束时间
        Date contractEndTime = DateUtils.parseDate("2023-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(contractEndTime);

        // 定义固定账单周期
        List<BillCycleCalculator.BillCycleVO> fixedCycles = new ArrayList<>();
        Date nodeStart1 = DateUtils.parseDate("2022-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeStart1 = DateUtils.convertDateToLocalDateTime(nodeStart1);
        Date nodeEnd1 = DateUtils.parseDate("2023-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeEnd1 = DateUtils.convertDateToLocalDateTime(nodeEnd1);

        //固定账期的时间阶段
        fixedCycles.add(new BillCycleCalculator.BillCycleVO(localDateTimeNodeStart1, localDateTimeNodeEnd1));


        ContractPeriodBo contractPeriodBo = new ContractPeriodBo();
        contractPeriodBo.setContractManageId("1001");
        contractPeriodBo.setContractPeriodId("1002");
        Date start = DateUtils.parseDate("2022-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end = DateUtils.parseDate("2023-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo.setStartTime(start);
        contractPeriodBo.setEndTime(end);
        contractPeriodBo.setPayment(new BigDecimal(1100));
        contractPeriodBo.setWholeFlag("1");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo);

        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        List<BillCycleBo> billCycleBos = BillCycleCalculator.adujustContractPeriod(localDateTimeStart, localDateTimeEnd, fixedCycles);


        List<ContractPeriodVo> monthContractPeriodVo = new ArrayList<>();

        for (BillCycleBo billCycleBo : billCycleBos) {
            String wholeFlag = billCycleBo.getWholeFlag();
            if ("0".equals(wholeFlag)) {
                List<ContractPeriodVo> contractPeriod = PeriodCalculator.getContractPeriod(billCycleBo.getStartTime(), billCycleBo.getEndTime(), 1);
                monthContractPeriodVo.addAll(contractPeriod);
            }
            if ("1".equals(wholeFlag)) {
                ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
                Date startTime = billCycleBo.getStartTime();
                contractPeriodVo.setPeriodStart(startTime);
                contractPeriodVo.setPeriodEnd(billCycleBo.getEndTime());
                contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, startTime));
                monthContractPeriodVo.add(contractPeriodVo);
            }
        }

        //账期
        for (ContractPeriodVo contractPeriodVo : monthContractPeriodVo) {
            System.out.println(contractPeriodVo);
        }

        //账期及金额
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doMonthCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" +
                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" +
                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testMonthCalculatorV2() throws ParseException {

        //合同开始时间
        Date contractStartTime = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(contractStartTime);
        //合同结束时间
        Date contractEndTime = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(contractEndTime);

        // 定义固定账单周期
        List<BillCycleCalculator.BillCycleVO> fixedCycles = new ArrayList<>();
        Date nodeStart1 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeStart1 = DateUtils.convertDateToLocalDateTime(nodeStart1);
        Date nodeEnd1 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeEnd1 = DateUtils.convertDateToLocalDateTime(nodeEnd1);

        //固定账期的时间阶段
        fixedCycles.add(new BillCycleCalculator.BillCycleVO(localDateTimeNodeStart1, localDateTimeNodeEnd1));

        //所有收费阶段信息
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("0");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);


        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        List<BillCycleBo> billCycleBos = BillCycleCalculator.adujustContractPeriod(localDateTimeStart, localDateTimeEnd, fixedCycles);


        List<ContractPeriodVo> monthContractPeriodVo = new ArrayList<>();

        for (BillCycleBo billCycleBo : billCycleBos) {
            String wholeFlag = billCycleBo.getWholeFlag();
            if ("0".equals(wholeFlag)) {
                List<ContractPeriodVo> contractPeriod = PeriodCalculator.getContractPeriod(billCycleBo.getStartTime(), billCycleBo.getEndTime(), 1);
                monthContractPeriodVo.addAll(contractPeriod);
            }
            if ("1".equals(wholeFlag)) {
                ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
                Date startTime = billCycleBo.getStartTime();
                contractPeriodVo.setPeriodStart(startTime);
                contractPeriodVo.setPeriodEnd(billCycleBo.getEndTime());
                contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, startTime));
                monthContractPeriodVo.add(contractPeriodVo);
            }
        }

        //账期
        for (ContractPeriodVo contractPeriodVo : monthContractPeriodVo) {
            System.out.println(contractPeriodVo);
        }

        //账期及金额
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doMonthCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" +
                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->"
                    + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testSeasonCalculator() throws ParseException {
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("0");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);

        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        Date start = DateUtils.parseDate("2024-04-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end = DateUtils.parseDate("2024-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);


        //月度 获取所有账单的开始时间与结束时间
        List<ContractPeriodVo> seasonContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 3);
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doSeasonCalculate(sortedVos, seasonContractPeriodVo);
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testSeasonCalculatorV1() throws ParseException {

        //合同开始时间
        Date contractStartTime = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(contractStartTime);
        //合同结束时间
        Date contractEndTime = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(contractEndTime);

        // 定义固定账单周期
        List<BillCycleCalculator.BillCycleVO> fixedCycles = new ArrayList<>();
        Date nodeStart1 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeStart1 = DateUtils.convertDateToLocalDateTime(nodeStart1);
        Date nodeEnd1 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeEnd1 = DateUtils.convertDateToLocalDateTime(nodeEnd1);

        //固定账期的时间阶段
        fixedCycles.add(new BillCycleCalculator.BillCycleVO(localDateTimeNodeStart1, localDateTimeNodeEnd1));

        //所有收费阶段信息
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("0");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);


        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        List<BillCycleBo> billCycleBos = BillCycleCalculator.adujustContractPeriod(localDateTimeStart, localDateTimeEnd, fixedCycles);


        List<ContractPeriodVo> monthContractPeriodVo = new ArrayList<>();

        for (BillCycleBo billCycleBo : billCycleBos) {
            String wholeFlag = billCycleBo.getWholeFlag();
            if ("0".equals(wholeFlag)) {
                List<ContractPeriodVo> contractPeriod = PeriodCalculator.getContractPeriod(billCycleBo.getStartTime(), billCycleBo.getEndTime(), 3);
                monthContractPeriodVo.addAll(contractPeriod);
            }
            if ("1".equals(wholeFlag)) {
                ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
                Date startTime = billCycleBo.getStartTime();
                contractPeriodVo.setPeriodStart(startTime);
                contractPeriodVo.setPeriodEnd(billCycleBo.getEndTime());
                contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, startTime));
                monthContractPeriodVo.add(contractPeriodVo);
            }
        }

        //月度 获取所有账单的开始时间与结束时间
        //monthContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 1);

        //账期
        for (ContractPeriodVo contractPeriodVo : monthContractPeriodVo) {
            System.out.println(contractPeriodVo);
        }

        //账期及金额
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doSeasonCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testHalfYearCalculator() throws ParseException {
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("1");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);

        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        Date start = DateUtils.parseDate("2024-04-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end = DateUtils.parseDate("2024-09-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);


        //月度 获取所有账单的开始时间与结束时间
        List<ContractPeriodVo> seasonContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 6);
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doHalfYearCalculate(sortedVos, seasonContractPeriodVo);
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testHalfYearCalculatorV1() throws ParseException {

        //合同开始时间
        Date contractStartTime = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(contractStartTime);
        //合同结束时间
        Date contractEndTime = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(contractEndTime);

        // 定义固定账单周期
        List<BillCycleCalculator.BillCycleVO> fixedCycles = new ArrayList<>();
        Date nodeStart1 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeStart1 = DateUtils.convertDateToLocalDateTime(nodeStart1);
        Date nodeEnd1 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeEnd1 = DateUtils.convertDateToLocalDateTime(nodeEnd1);

        //固定账期的时间阶段
        fixedCycles.add(new BillCycleCalculator.BillCycleVO(localDateTimeNodeStart1, localDateTimeNodeEnd1));

        //所有收费阶段信息
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("0");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);


        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        List<BillCycleBo> billCycleBos = BillCycleCalculator.adujustContractPeriod(localDateTimeStart, localDateTimeEnd, fixedCycles);


        List<ContractPeriodVo> monthContractPeriodVo = new ArrayList<>();

        for (BillCycleBo billCycleBo : billCycleBos) {
            String wholeFlag = billCycleBo.getWholeFlag();
            if ("0".equals(wholeFlag)) {
                List<ContractPeriodVo> contractPeriod = PeriodCalculator.getContractPeriod(billCycleBo.getStartTime(), billCycleBo.getEndTime(), 6);
                monthContractPeriodVo.addAll(contractPeriod);
            }
            if ("1".equals(wholeFlag)) {
                ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
                Date startTime = billCycleBo.getStartTime();
                contractPeriodVo.setPeriodStart(startTime);
                contractPeriodVo.setPeriodEnd(billCycleBo.getEndTime());
                contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, startTime));
                monthContractPeriodVo.add(contractPeriodVo);
            }
        }

        //月度 获取所有账单的开始时间与结束时间
        //monthContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 1);

        //账期
        for (ContractPeriodVo contractPeriodVo : monthContractPeriodVo) {
            System.out.println(contractPeriodVo);
        }

        //账期及金额
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doHalfYearCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testYearCalculator() throws ParseException {
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("1");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);

        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        Date start = DateUtils.parseDate("2024-04-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end = DateUtils.parseDate("2025-02-02 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);

        //月度 获取所有账单的开始时间与结束时间
        List<ContractPeriodVo> seasonContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 3);
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doHalfYearCalculate(sortedVos, seasonContractPeriodVo);
        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }


    @Test
    public void testYearCalculatorV1() throws ParseException {

        //合同开始时间
        Date contractStartTime = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeStart = DateUtils.convertDateToLocalDateTime(contractStartTime);
        //合同结束时间
        Date contractEndTime = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeEnd = DateUtils.convertDateToLocalDateTime(contractEndTime);

        // 定义固定账单周期
        List<BillCycleCalculator.BillCycleVO> fixedCycles = new ArrayList<>();
        Date nodeStart1 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeStart1 = DateUtils.convertDateToLocalDateTime(nodeStart1);
        Date nodeEnd1 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        LocalDateTime localDateTimeNodeEnd1 = DateUtils.convertDateToLocalDateTime(nodeEnd1);

        //固定账期的时间阶段
        fixedCycles.add(new BillCycleCalculator.BillCycleVO(localDateTimeNodeStart1, localDateTimeNodeEnd1));

        //所有收费阶段信息
        ContractPeriodBo contractPeriodBo1 = new ContractPeriodBo();
        contractPeriodBo1.setContractManageId("1001");
        contractPeriodBo1.setContractPeriodId("1002");
        Date start1 = DateUtils.parseDate("2015-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end1 = DateUtils.parseDate("2016-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo1.setStartTime(start1);
        contractPeriodBo1.setEndTime(end1);
        contractPeriodBo1.setPayment(new BigDecimal(0));
        contractPeriodBo1.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo7 = new ContractPeriodBo();
        contractPeriodBo7.setContractManageId("1001");
        contractPeriodBo7.setContractPeriodId("1008");
        Date start7 = DateUtils.parseDate("2016-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end7 = DateUtils.parseDate("2016-06-30 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo7.setStartTime(start7);
        contractPeriodBo7.setEndTime(end7);
        contractPeriodBo7.setPayment(new BigDecimal(60000));
        contractPeriodBo7.setWholeFlag("1");


        ContractPeriodBo contractPeriodBo2 = new ContractPeriodBo();
        contractPeriodBo2.setContractManageId("1001");
        contractPeriodBo2.setContractPeriodId("1003");
        Date start2 = DateUtils.parseDate("2016-07-01 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end2 = DateUtils.parseDate("2018-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo2.setStartTime(start2);
        contractPeriodBo2.setEndTime(end2);
        contractPeriodBo2.setPayment(new BigDecimal(60000));
        contractPeriodBo2.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo3 = new ContractPeriodBo();
        contractPeriodBo3.setContractManageId("1001");
        contractPeriodBo3.setContractPeriodId("1004");
        Date start3 = DateUtils.parseDate("2018-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end3 = DateUtils.parseDate("2020-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo3.setStartTime(start3);
        contractPeriodBo3.setEndTime(end3);
        contractPeriodBo3.setPayment(new BigDecimal(66000));
        contractPeriodBo3.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo4 = new ContractPeriodBo();
        contractPeriodBo4.setContractManageId("1001");
        contractPeriodBo4.setContractPeriodId("1005");
        Date start4 = DateUtils.parseDate("2020-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end4 = DateUtils.parseDate("2022-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo4.setStartTime(start4);
        contractPeriodBo4.setEndTime(end4);
        contractPeriodBo4.setPayment(new BigDecimal(72600));
        contractPeriodBo4.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo5 = new ContractPeriodBo();
        contractPeriodBo5.setContractManageId("1001");
        contractPeriodBo5.setContractPeriodId("1006");
        Date start5 = DateUtils.parseDate("2022-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end5 = DateUtils.parseDate("2024-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo5.setStartTime(start5);
        contractPeriodBo5.setEndTime(end5);
        contractPeriodBo5.setPayment(new BigDecimal(79860));
        contractPeriodBo5.setWholeFlag("0");


        ContractPeriodBo contractPeriodBo6 = new ContractPeriodBo();
        contractPeriodBo6.setContractManageId("1001");
        contractPeriodBo6.setContractPeriodId("1007");
        Date start6 = DateUtils.parseDate("2024-05-16 00:00:00", DateUtils.YYYY_MM_DD_HH_MM_SS);
        Date end6 = DateUtils.parseDate("2025-05-15 23:59:59", DateUtils.YYYY_MM_DD_HH_MM_SS);
        contractPeriodBo6.setStartTime(start6);
        contractPeriodBo6.setEndTime(end6);
        contractPeriodBo6.setPayment(new BigDecimal(87846));
        contractPeriodBo6.setWholeFlag("0");


        List<ContractPeriodBo> vos = new ArrayList<>();
        vos.add(contractPeriodBo1);
        vos.add(contractPeriodBo2);
        vos.add(contractPeriodBo3);
        vos.add(contractPeriodBo4);
        vos.add(contractPeriodBo5);
        vos.add(contractPeriodBo6);
        vos.add(contractPeriodBo7);


        List<ContractPeriodBo> sortedVos = PeriodCalculator.getContractPeriodVos(vos);

        List<BillCycleBo> billCycleBos = BillCycleCalculator.adujustContractPeriod(localDateTimeStart, localDateTimeEnd, fixedCycles);


        List<ContractPeriodVo> monthContractPeriodVo = new ArrayList<>();

        for (BillCycleBo billCycleBo : billCycleBos) {
            String wholeFlag = billCycleBo.getWholeFlag();
            if ("0".equals(wholeFlag)) {
                List<ContractPeriodVo> contractPeriod = PeriodCalculator
                        .getContractPeriod(billCycleBo.getStartTime(), billCycleBo.getEndTime(), 12);
                monthContractPeriodVo.addAll(contractPeriod);
            }
            if ("1".equals(wholeFlag)) {
                ContractPeriodVo contractPeriodVo = new ContractPeriodVo();
                Date startTime = billCycleBo.getStartTime();
                contractPeriodVo.setPeriodStart(startTime);
                contractPeriodVo.setPeriodEnd(billCycleBo.getEndTime());
                contractPeriodVo.setYearMonth(DateUtils.parseDateToStr(DateUtils.YYYY_MM, startTime));
                monthContractPeriodVo.add(contractPeriodVo);
            }
        }

        //月度 获取所有账单的开始时间与结束时间
        //monthContractPeriodVo = PeriodCalculator.getContractPeriod(start, end, 1);

        //账期
        for (ContractPeriodVo contractPeriodVo : monthContractPeriodVo) {
            System.out.println(contractPeriodVo);
        }

        //账期及金额
        List<ContractPeriodVo> contractPeriodVos = MoneyCalculator.doYearCalculate(sortedVos, monthContractPeriodVo);

        for (ContractPeriodVo contractPeriodVo : contractPeriodVos) {
            System.out.println(contractPeriodVo.getMonthRent() + "-->" + contractPeriodVo.getYearMonth() + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodStart()) + "-->" + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, contractPeriodVo.getPeriodEnd()));
        }
    }

}

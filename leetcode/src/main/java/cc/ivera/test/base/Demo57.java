package cc.ivera.test.base;

import org.apache.commons.lang3.StringUtils;
import cc.ivera.model.vo.DocumentManageVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Demo57 {

    public static void main(String[] args) {
        List<DocumentManageVo> list = new ArrayList<>();

        DocumentManageVo vo1 = new DocumentManageVo();
        vo1.setBill_amount(new BigDecimal(1000));
        vo1.setAdjust_bill_amount(new BigDecimal(800));
        vo1.setBill_start_date(null);
        vo1.setBill_end_date(null);
        vo1.setAdjust_bill_start_date(null);
        vo1.setAdjust_bill_end_date(null);
        vo1.setContract_managet_id("1010");
        vo1.setDocument_manage_id("10101");
        vo1.setOperation("1");


        DocumentManageVo vo2 = new DocumentManageVo();
        vo2.setBill_amount(new BigDecimal(1000));
        vo2.setAdjust_bill_amount(new BigDecimal(800));
        vo2.setBill_start_date(null);
        vo2.setBill_end_date(null);
        vo2.setAdjust_bill_start_date(null);
        vo2.setAdjust_bill_end_date(null);
        vo2.setContract_managet_id("1010");
        vo2.setDocument_manage_id("10102");
        vo2.setOperation("2");


        DocumentManageVo vo3 = new DocumentManageVo();
        vo3.setBill_amount(new BigDecimal(1000));
        vo3.setAdjust_bill_amount(new BigDecimal(800));
        vo3.setBill_start_date(null);
        vo3.setBill_end_date(null);
        vo3.setAdjust_bill_start_date(null);
        vo3.setAdjust_bill_end_date(null);
        vo3.setContract_managet_id("1010");
        vo3.setDocument_manage_id("10103");
        vo3.setOperation(null);


        DocumentManageVo vo4 = new DocumentManageVo();
        vo4.setBill_amount(new BigDecimal(1000));
        vo4.setAdjust_bill_amount(new BigDecimal(800));
        vo4.setBill_start_date(null);
        vo4.setBill_end_date(null);
        vo4.setAdjust_bill_start_date(null);
        vo4.setAdjust_bill_end_date(null);
        vo4.setContract_managet_id("1010");
        vo4.setDocument_manage_id("10104");
        vo4.setOperation(null);

        list.add(vo1);
        list.add(vo2);
        list.add(vo3);
        list.add(vo4);

        //根据操作进行分组处理
        //参考地址https://blog.csdn.net/xilin6664/article/details/104570273
        Map<String, List<DocumentManageVo>> collect = list.stream()
                .filter(new Predicate<DocumentManageVo>() {
                    @Override
                    public boolean test(DocumentManageVo documentManageVo) {
                        if (StringUtils.isNotEmpty(documentManageVo.getOperation())) {
                            //保留
                            return true;
                        } else {
                            //移除
                            return false;
                        }
                    }
                })
                .collect(Collectors.groupingBy(vo -> vo.getOperation()));

        System.out.println(collect.keySet());

    }


}

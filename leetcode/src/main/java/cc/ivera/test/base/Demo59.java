package cc.ivera.test.base;

import org.junit.Assert;
import org.junit.Test;
import cc.ivera.model.pojo.BasePropertyType;
import cc.ivera.model.vo.TreeBean;
import cc.ivera.util.DateUtils;
import cc.ivera.util.TreeBuilder3;
import cc.ivera.util.TreeNode3;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Demo59 {

    private static void print(String... args) {
        System.out.println("args.length = " + args.length);
    }

    public static Date calculatePayDeadline(Date startDate, int days) throws ParseException {
        //获取账单的开始年月，然后获取预设日期，判断是否在在本月的日期内，如果时设置成该日期，
        //如果不是，设置成下个月的这个日期。
        String s = DateUtils.parseDateToStr(DateUtils.YYYY_MM, startDate);
        if (days < 10) {
            s = s + "-" + "0" + days;
        } else {
            s = s + "-" + days;
        }
        Date date = DateUtils.parseDate(s, DateUtils.YYYY_MM_DD);
        if (date.getTime() < startDate.getTime()) {
            //获取下个月的日期
            LocalDate localDate = DateUtils.convertDateToLocalDate(startDate);
            LocalDate nextDate = localDate.plusMonths(1);
            Date deadLine = DateUtils.toDate(nextDate);
            String expected = DateUtils.parseDateToStr(DateUtils.YYYY_MM, deadLine);
            if (days < 10) {
                expected = expected + "-" + "0" + days;
            } else {
                expected = expected + "-" + days;
            }
            date = DateUtils.parseDate(expected, DateUtils.YYYY_MM_DD);
            return DateUtils.endDayPlus(date, 1);
        } else {
            return DateUtils.endDayPlus(date, 1);
        }
    }

    @Test
    public void test1() {
        String[] params = {"1", "2", "3"};
        print(params);
    }

    @Test
    public void test2() {
        String s = "123";
        String[] split = s.split(";");
        Assert.assertEquals("123", split[0]);
    }

    @Test
    public void test3() {
        String s = ";";
        try {
            String[] split = s.split(";");
            System.out.println(split[0]);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    //算法:递归算法，stream的映射构建
    @Test
    public void test4() {
        BasePropertyType data1 = new BasePropertyType();
        TreeBean bean1 = new TreeBean();
        BasePropertyType data2 = new BasePropertyType();
        TreeBean bean2 = new TreeBean();
        BasePropertyType data3 = new BasePropertyType();
        TreeBean bean3 = new TreeBean();
        BasePropertyType data4 = new BasePropertyType();
        TreeBean bean4 = new TreeBean();
        BasePropertyType data5 = new BasePropertyType();
        TreeBean bean5 = new TreeBean();
        BasePropertyType data6 = new BasePropertyType();
        TreeBean bean6 = new TreeBean();
        BasePropertyType data7 = new BasePropertyType();
        TreeBean bean7 = new TreeBean();
        BasePropertyType data8 = new BasePropertyType();
        TreeBean bean8 = new TreeBean();

        data1.setProperty_type_id("1001");
        data1.setType_level(1);
        data1.setProperty_type_name("百事高（广州）实业有限公司");
        data1.setProperty_type_code("102");

        bean1.setId("1001");
        bean1.setPId("");
        bean1.setName("百事高（广州）实业有限公司");
        bean1.setParent(false);
        bean1.setObj(data1);
        bean1.setSelected(false);

        data2.setProperty_type_id("10011048");
        data2.setType_level(2);
        data2.setProperty_type_name("西陇创新园");
        data2.setProperty_type_code("XL01");


        bean2.setId("10011048");
        bean2.setPId("");
        bean2.setName("西陇创新园");
        bean2.setParent(false);
        bean2.setObj(data2);
        bean2.setSelected(false);

        data3.setProperty_type_id("100110481001");
        data3.setType_level(3);
        data3.setProperty_type_name("1栋");
        data3.setProperty_type_code("XL0101");

        bean3.setId("100110481001");
        bean3.setPId("");
        bean3.setName("1栋");
        bean3.setParent(false);
        bean3.setObj(data3);
        bean3.setSelected(false);

        data4.setProperty_type_id("100110481002");
        data4.setType_level(3);
        data4.setProperty_type_name("2栋");
        data4.setProperty_type_code("XL0102");

        bean4.setId("100110481002");
        bean4.setPId("");
        bean4.setName("2栋");
        bean4.setParent(false);
        bean4.setObj(data4);
        bean4.setSelected(false);

        data5.setProperty_type_id("1003");
        data5.setType_level(1);
        data5.setProperty_type_name("广州科泰智慧有限公司");
        data5.setProperty_type_code("103");


        bean5.setId("1003");
        bean5.setPId("");
        bean5.setName("广州科泰智慧有限公司");
        bean5.setParent(false);
        bean5.setObj(data5);
        bean5.setSelected(false);

        data6.setProperty_type_id("10031001");
        data6.setType_level(2);
        data6.setProperty_type_name("创维平面厂房");
        data6.setProperty_type_code("1030101");


        bean6.setId("10031001");
        bean6.setPId("");
        bean6.setName("创维平面厂房");
        bean6.setParent(false);
        bean6.setObj(data6);
        bean6.setSelected(false);


        data7.setProperty_type_id("1004");
        data7.setType_level(1);
        data7.setProperty_type_name("广东和诚科技孵化器有限公司");
        data7.setProperty_type_code("104");

        bean7.setId("1004");
        bean7.setPId("");
        bean7.setName("广东和诚科技孵化器有限公司");
        bean7.setParent(false);
        bean7.setObj(data7);
        bean7.setSelected(false);

        data8.setProperty_type_id("10041048");
        data8.setType_level(2);
        data8.setProperty_type_name("和诚园区");
        data8.setProperty_type_code("HC01");

        bean8.setId("10041048");
        bean8.setPId("");
        bean8.setName("广东和诚科技孵化器有限公司");
        bean8.setParent(false);
        bean8.setObj(data8);
        bean8.setSelected(false);

        List<BasePropertyType> list = new ArrayList<>();
        list.add(data1);
        list.add(data2);
        list.add(data3);
        list.add(data4);
        list.add(data5);
        list.add(data6);
        list.add(data7);
        list.add(data8);

        Map<String, TreeNode3> nodeMap = new HashMap<>();

        List<TreeBean> treeBeans = new ArrayList<>();

        treeBeans.add(bean1);
        treeBeans.add(bean2);
        treeBeans.add(bean3);
        treeBeans.add(bean4);
        treeBeans.add(bean5);
        treeBeans.add(bean6);
        treeBeans.add(bean7);
        treeBeans.add(bean8);

        //使用stream构建单一映射器
        Map<String, TreeBean> treeBeanMap = treeBeans.stream().collect(Collectors.toMap(TreeBean::getId, Function.identity()));


        // 第一遍遍历：将数据添加到节点映射表中
        for (BasePropertyType data : list) {
            String typeId = data.getProperty_type_id();
            String parentId = TreeBuilder3.getParentId(typeId);
            int level = data.getType_level();
            TreeNode3 node = new TreeNode3(typeId, parentId, level);
            nodeMap.put(typeId, node);
        }

        List<TreeNode3> treeNodes = TreeBuilder3.buildTree(nodeMap);

        for (int i = 0; i < treeNodes.size(); i++) {
            TreeNode3.printParentNodes(treeNodes.get(i), nodeMap, treeBeanMap);
        }

        Collection<TreeBean> values = treeBeanMap.values();

        values.forEach(new Consumer<TreeBean>() {
            @Override
            public void accept(TreeBean treeBean) {
                System.out.println(treeBean);
            }
        });
    }

    @Test
    public void test5() {
        Double value = 3.141926D;
        String s = value.toString();
        BigDecimal bigDecimal = new BigDecimal(s);
        System.out.println(bigDecimal);
    }

    @Test
    public void test6() {
        String dateStr = "2024-02-01 00:00:00";
        Date date1 = DateUtils.parseDate(dateStr);
        Date date2 = DateUtils.endDayPlus(date1, 30);
        //1天内的结束时间
        System.out.println(date2);
    }

    @Test
    public void test7() throws ParseException {
        String dateStr = "2024-01-31 00:00:00";
        Date date1 = DateUtils.parseDate(dateStr);
        Date date2 = Demo59.calculatePayDeadline(date1, 30);
        System.out.println(date2);
    }

}

package cc.ivera.test.base;


import lombok.SneakyThrows;
import org.junit.Test;
import cc.ivera.test.entity.User;
import cc.ivera.util.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static cc.ivera.util.DateUtils.YYYY_MM_DD;
import static cc.ivera.util.DateUtils.YYYY_MM_DD_HH_MM_SS;

public class Demo23 {

    @Test
    public void testMain() throws ParseException {

        User user1 = User.builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23 10:23:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user2 = User.builder().id("id002").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23 18:23:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user3 = User.builder().id("id003").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23 19:23:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user4 = User.builder().id("id004").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23 16:23:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user5 = User.builder().id("id005").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23 00:00:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user6 = User.builder().id("id006").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11 00:00:00", YYYY_MM_DD_HH_MM_SS)).build();
        User user7 = User.builder().id("id007").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23 00:00:00", YYYY_MM_DD_HH_MM_SS)).build();

        List<User> users = new ArrayList<>();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);

        Map<String, List<User>> collect = users.stream().collect(Collectors.groupingBy(user -> new SimpleDateFormat("yyyy-MM-dd").format(user.getDob())));
        Set<String> strings = collect.keySet();

        List<String> sortedKeys = strings.stream().sorted(new Comparator<String>() {
            @SneakyThrows
            @Override
            public int compare(String o1, String o2) {
                Date date1 = DateUtils.parseDate(o1, YYYY_MM_DD);
                Date date2 = DateUtils.parseDate(o2, YYYY_MM_DD);
                if (date1.getTime() < date2.getTime()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }).collect(Collectors.toList());


        Map<String, List<User>> map = new LinkedHashMap<>();

        for (String sortedKey : sortedKeys) {
            List<User> tempList = collect.get(sortedKey);
            List<User> newList = tempList.stream().sorted(new Comparator<User>() {
                @Override
                public int compare(User o1, User o2) {
                    if (o1.getDob().getTime() < o2.getDob().getTime()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }).collect(Collectors.toList());
            map.put(sortedKey, newList);
        }

        Set<String> keys = map.keySet();

        for (String key : keys) {
            System.out.println(key + "-->" + map.get(key));
        }

    }


    @Test
    public void testCreatingStream() {
        //Stream流只能使用一次
        //数组
        String[] str = {"hello", "world"};
        Arrays.stream(str).forEach(s -> System.out.println(s));

        //单列集合
        List<String> list = new ArrayList<>();
        //数组转换成集合
        Arrays.asList(str);
    }

    /**
     * stream流过滤
     */
    @Test
    public void testFilter() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);


        //过滤年龄少于18岁的 使用stream流不用影响原list,使用collect收集器进行收集查找效果
        List<User> collect = users.stream().filter(new Predicate<User>() {
            @Override
            public boolean test(User user) {
                return user.getAge() >= 18;
            }
        }).collect(Collectors.toList());


        //使用stream流不用影响原list
        users.stream().forEach(user -> System.out.println(user));
        System.out.println("----------------------------------------------");
        collect.stream().forEach(user -> System.out.println(user));

    }


    /**
     * stream流list转换Map
     */
    @Test
    public void testMap() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);

        //以用户ID作为key,user作为value
        Map<String, User> collect1 = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        User id0001User = collect1.get("id001");

        System.out.println(id0001User);


        System.out.println("-----------------------------------------");

        collect1.forEach(new BiConsumer<String, User>() {
            @Override
            public void accept(String s, User user) {
                System.out.println(collect1.get(s));
            }
        });


        Map<String, String> collect2 = users.stream().collect(Collectors.toMap(User::getId, User::getName, (key1, key2) -> key2));

        String id005Name = collect2.get("id005");

        System.out.println("----------------------------------------");
        System.out.println(id005Name);

    }


    /**
     * stream流分组(根据年龄)
     */
    @Test
    public void testGroupBy() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);


        Map<Integer, List<User>> collect = users.stream().collect(Collectors.groupingBy(User::getAge));


        //根据年龄进行分组
        collect.forEach(new BiConsumer<Integer, List<User>>() {
            @Override
            public void accept(Integer age, List<User> users) {
                System.out.println(collect.get(age));
            }
        });


    }


    /**
     * stream流分组(根据出生日期年月分组)
     */
    @Test
    public void testGroupByDateFormat() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);


        Map<String, List<User>> collect = users.stream().collect(Collectors.groupingBy(user -> new SimpleDateFormat("yyyy-MM").format(user.getDob())));


        //根据年龄进行分组
        collect.forEach(new BiConsumer<String, List<User>>() {
            @Override
            public void accept(String yyyyMM, List<User> users) {
                System.out.println(collect.get(yyyyMM));
            }
        });

    }


    /**
     * stream流排序(根据年龄,成绩)
     */
    @Test
    public void testSort() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).score(100).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).score(98).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).score(88).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).score(98).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).score(88).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).score(100).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).score(96).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).score(100).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).score(100).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).score(90).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);

        //根据年龄从小到大排序
        List<User> collect1 = users.stream().sorted(Comparator.comparing(User::getAge))
                .collect(Collectors.toList());


        collect1.forEach(user -> System.out.println(user));

        System.out.println("-----------------------------------------------------------------");

        //根据年龄从大到小排序
        List<User> collect2 = users.stream().sorted(Comparator.comparing(User::getAge).reversed())
                .collect(Collectors.toList());

        collect2.forEach(user -> System.out.println(user));


        System.out.println("-----------------------------------------------------------------");


        //现根据年龄排序从大到小排序 如果年龄相同根据分数从高到低排序
        List<User> collect3 = users.stream().sorted(Comparator.comparing(User::getAge)
                .reversed()
                .thenComparing(Comparator.comparing(User::getScore).reversed())
        ).collect(Collectors.toList());

        collect3.stream().forEach(user -> System.out.println(user));


    }


    /**
     * stream流的聚合取值
     */
    @Test
    public void testReduce() {

        //求和
        List<Integer> list = Arrays.asList(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        Integer sum = list.stream().reduce((num1, num2) -> num1 + num2).get();
        System.out.println(sum);


        System.out.println("-------------------------------------");

        //求最小值
        Integer min = list.stream().reduce(new BinaryOperator<Integer>() {
            @Override
            public Integer apply(Integer num1, Integer num2) {
                if (num1 >= num2) {
                    return num2;
                } else {
                    return num1;
                }
            }
        }).get();

        System.out.println(min);

    }


    /***
     * stream流分页
     */
    @Test
    public void testPage() throws ParseException {

        List<User> users = new ArrayList<User>(10);

        User user1 = new User().builder().id("id001").name("船长").age(23).gender("1").dob(DateUtils.parseDate("1999-10-23", YYYY_MM_DD)).build();
        User user2 = new User().builder().id("id002").name("阿卡丽").age(16).gender("0").dob(DateUtils.parseDate("2006-10-23", YYYY_MM_DD)).build();
        User user3 = new User().builder().id("id003").name("凯南").age(16).gender("1").dob(DateUtils.parseDate("2006-11-11", YYYY_MM_DD)).build();
        User user4 = new User().builder().id("id004").name("沙皇").age(17).gender("1").dob(DateUtils.parseDate("2005-01-18", YYYY_MM_DD)).build();
        User user5 = new User().builder().id("id005").name("妖姬").age(24).gender("0").dob(DateUtils.parseDate("1998-01-18", YYYY_MM_DD)).build();
        User user6 = new User().builder().id("id006").name("剑魔").age(18).gender("1").dob(DateUtils.parseDate("2004-10-23", YYYY_MM_DD)).build();
        User user7 = new User().builder().id("id007").name("猫咪").age(18).gender("0").dob(DateUtils.parseDate("2004-06-23", YYYY_MM_DD)).build();
        User user8 = new User().builder().id("id008").name("青刚影").age(20).gender("0").dob(DateUtils.parseDate("2002-10-23", YYYY_MM_DD)).build();
        User user9 = new User().builder().id("id009").name("盲僧").age(24).gender("1").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();
        User user10 = new User().builder().id("id010").name("露露").age(24).gender("0").dob(DateUtils.parseDate("1998-10-23", YYYY_MM_DD)).build();

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);
        users.add(user5);
        users.add(user6);
        users.add(user7);
        users.add(user8);
        users.add(user9);
        users.add(user10);

        List<User> page = users.stream().skip(0).limit(5).collect(Collectors.toList());

        System.out.println(users.size());

        System.out.println(page.size());

        page.stream().forEach(user -> System.out.println(user));

    }


    /***
     * map集合根据value进行分组
     */
    @Test
    public void testMapGroupByValue() {
        Map<String, String> map = new HashMap<>();
        Map<String, Set<String>> groupMap = new HashMap<>();
        map.put(null, null);
        map.put(null, null);
        map.put("c", null);
        map.put("c", "3");
        map.put("e", "4");
        map.put("f", "5");
        map.put("G", "4");
        map.put("h", "5");

        //根据value分组之前
        map.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                System.out.println(key + "->" + value);
            }
        });


        System.out.println("----------------------------------------------------");


        map.forEach(new BiConsumer<String, String>() {
            @Override
            public void accept(String key, String value) {
                if (groupMap.containsKey(value)) {
                    groupMap.get(value).add(key);
                } else {
                    Set<String> values = new HashSet<>();
                    values.add(key);
                    groupMap.put(value, values);
                }
            }
        });

        //根据value分组之后
        groupMap.forEach(new BiConsumer<String, Set<String>>() {
            @Override
            public void accept(String value, Set<String> keys) {
                System.out.println(value + "->" + keys);
            }
        });
    }


}




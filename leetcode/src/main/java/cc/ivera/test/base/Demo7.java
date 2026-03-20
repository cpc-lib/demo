package cc.ivera.test.base;

import cn.hutool.core.date.StopWatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 两个数据组如何快速去重
 */
public class Demo7 {


    public static List<String> getTestList() {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 100000; i++) {
            list.add(String.valueOf(i));
        }
        for (int i = 100000; i >= 1; i--) {
            list.add(String.valueOf(i));
        }
        return list;
    }


    /**
     * 使用 list.contain 去重
     *
     * @param testList
     */
    private static void useContainDistinct(List<String> testList) {
        System.out.println("contains 开始去重，条数：" + testList.size());
        List<String> testListDistinctResult = new ArrayList<>();
        for (String str : testList) {
            if (!testListDistinctResult.contains(str)) {
                testListDistinctResult.add(str);
            }
        }
        System.out.println("contains 去重完毕，条数：" + testListDistinctResult.size());
    }


    public static void main(String[] args) {
        //test0();
        test1();
    }


    /**
     * 使用set去重
     *
     * @param testList
     */
    private static void useSetDistinct(List<String> testList) {
        System.out.println("HashSet.add 开始去重，条数：" + testList.size());
        List<String> testListDistinctResult = new ArrayList<>(new HashSet(testList));
        System.out.println("HashSet.add 去重完毕，条数：" + testListDistinctResult.size());
    }


    private static void test0() {
        List<String> testList = getTestList();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        useContainDistinct(testList);
        stopWatch.stop();
        System.out.println("去重 最终耗时" + stopWatch.getTotalTimeMillis());
    }


    private static void test1() {
        List<String> testList = getTestList();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        useSetDistinct(testList);
        stopWatch.stop();
        System.out.println("去重 最终耗时" + stopWatch.getTotalTimeMillis());
    }


}

package cc.ivera.test.base;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class Demo14 {
    @Test
    public void test1() {
        Set<String> set = new TreeSet<>();
        String s = null;
        set.add(s);
    }

    @Test
    public void test2() {
        Set<String> set = new HashSet<>();
        String s = null;
        set.add(s);
    }


    @Test
    public void test3() {
        Set<String> set = new HashSet<>();
        String s0 = null;
        String s1 = null;
        set.add(s0);
        set.add(s1);
        set.stream().forEach(s -> System.out.println(s));
    }


    @Test
    public void test4() {
        Set<Integer> set = new TreeSet<>();
        Integer s0 = 100;
        Integer s1 = 98;
        Integer s2 = 98;
        Integer s3 = 97;
        Integer s4 = 95;
        set.add(s0);
        set.add(s1);
        set.add(s2);
        set.add(s3);
        set.add(s4);
        set.stream().forEach(s -> System.out.println(s));
    }
}

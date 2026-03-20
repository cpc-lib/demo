package cc.ivera.test.base;

import java.util.*;

public class Demo58 {
    public static void main(String[] args) {
        //treeSet 不允许存储null值,值不可以重复
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("");
        //treeSet.add(null);
        treeSet.add("1");
        treeSet.add("1");
        System.out.println(treeSet);

        //hashSet 允许存储null值,值不可以重复
        Set<String> hashSet = new HashSet<>();
        hashSet.add("");
        hashSet.add(null);
        hashSet.add("1");
        hashSet.add("1");
        System.out.println(hashSet);

        //arrayList 允许存储null值,值可以重复 数组
        List<String> arrayList = new ArrayList<>();
        arrayList.add("");
        arrayList.add(null);
        arrayList.add("1");
        arrayList.add("1");
        System.out.println(arrayList);

        //linkedList 允许存储null值,值可以重复 链表
        List<String> linkedList = new LinkedList<>();
        linkedList.add("");
        linkedList.add(null);
        linkedList.add("1");
        linkedList.add("1");
        System.out.println(linkedList);
    }
}

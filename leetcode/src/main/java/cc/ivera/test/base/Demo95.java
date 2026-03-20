package cc.ivera.test.base;

import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

public class Demo95 {
    public static void main(String[] args) {
        StringJoiner sj = new StringJoiner(",");
        String s1 = "";
        String s2 = "";
        Set<String> treeSet = new TreeSet<>();
        treeSet.add(s1);
        treeSet.add(s2);
        if (treeSet.size() > 0) {
            for (String s : treeSet) {
                sj.add(s);
            }
        }
        //sj.add(s1).add(s2);
        System.out.println(",".equals(sj.toString()));
    }
}

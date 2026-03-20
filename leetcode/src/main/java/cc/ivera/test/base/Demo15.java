package cc.ivera.test.base;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Demo15 {
    @Test
    public void test1() {
        String sets = "112,113,114,115,";
        System.out.println(sets.substring(0, sets.length() - 1));
    }


    @Test
    public void test2() {
        List<String> sets = new ArrayList<>();
        sets.add("123");
        sets.add("124");
        sets.add("125");
        sets.add("126");
        sets.add("127");
        sets.add("128");
        StringBuilder stringBuilder = new StringBuilder();
        sets.forEach(new Consumer<String>() {
            @Override
            public void accept(String s) {
                stringBuilder.append("'").append(s).append("',");
            }
        });
        String setStr = stringBuilder.toString();
        System.out.println(stringBuilder.toString().substring(0, setStr.length() - 1));
    }
}

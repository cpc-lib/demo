package cc.ivera.test.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class Demo34 {
    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        String key = "key";
        String value = "value";
        for (int i = 0; i < 10000000; i++) {
            map.put(key + i, value + i);
        }
        long start = System.currentTimeMillis();
        test1(map);
        long end = System.currentTimeMillis();
        System.out.println(end - start + "ms");

    }

    private static void test1(Map<String, Object> map) {
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String s = entry.getKey();
            System.out.println(s);
            System.out.println(map.get(s));
        }
    }

    private static void test2(Map<String, Object> map) {
        Set<String> keys = map.keySet();
        for (String key : keys) {
            System.out.println(key);
            System.out.println(map.get(key));
        }
    }


    private static void test3(Map<String, Object> map) {
        map.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String s, Object o) {
                System.out.println(s);
                System.out.println(map.get(s));
            }
        });
    }
}

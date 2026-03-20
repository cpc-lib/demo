package cc.ivera.test.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Demo101 {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("world!");
        Iterator<String> it = list. iterator();
        while(it. hasNext()){
            String obj = it. next();
            list.remove(obj);
            System. out. println(obj);
        }
    }
}

package cc.ivera.test.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Demo102 {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list. add("x");
        Collection<String> clist = Collections. unmodifiableCollection(list);
        clist. add("y"); // 运行时此行报错
        System. out. println(list. size());
    }
}

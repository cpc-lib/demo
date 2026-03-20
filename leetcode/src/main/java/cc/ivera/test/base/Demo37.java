package cc.ivera.test.base;

import cc.ivera.model.pojo.Body;
import cc.ivera.util.GsonUtil;

import java.util.List;

public class Demo37 {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String json = """
                            [{"name":"zhangsan","age":18},{"name":"lisi","age":23}]
                            """;
                    List<Body> ts = GsonUtil.parseJsonArrToList(json, Body.class);
                    for (Body t : ts) {
                        System.out.println(Thread.currentThread().getName() + ":" + t);
                    }
                }
            });
            thread.start();
        }
    }
}

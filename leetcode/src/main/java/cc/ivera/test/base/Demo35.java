package cc.ivera.test.base;

import com.alibaba.fastjson.JSONArray;
import cc.ivera.model.pojo.Body;
import cc.ivera.util.FastJsonUtil;
import cc.ivera.util.GsonUtil;

import java.util.List;
import java.util.function.Consumer;

public class Demo35 {

    public static void main(String[] args) {
        String json = """
                [{"name":"zhangsan","age":18},{"name":"lisi","age":23}]
                """;
        //初始化JSONArray
        JSONArray jsonArray = JSONArray.parseArray(json);
        //打印jsonArray
        System.out.println(jsonArray.toJSONString());
        //转List
        List<Body> list = JSONArray.parseArray(jsonArray.toJSONString(), Body.class);
        //打印List<Body>
        System.out.println(list.toString());

        List<Body> bodies1 = FastJsonUtil.parseJsonArrToList(json, Body.class);
        List<Body> bodies2 = GsonUtil.parseJsonArrToList(json, Body.class);
        bodies1.forEach(new Consumer<Body>() {
            @Override
            public void accept(Body body) {
                System.out.println(body);
            }
        });
        System.out.println("-------------------------------------------------");
        bodies1.forEach(new Consumer<Body>() {
            @Override
            public void accept(Body body) {
                System.out.println(body);
            }
        });
    }
}

import com.alibaba.fastjson.JSON;
import cc.ivera.common.bean.ResponseBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class BaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void test1() throws JsonProcessingException {
        Map<String,String> result = new HashMap<>();
        result.put("name","action2");
        result.put("no","2");
        result.put("address","岳阳");

        //MyBean myBean = new MyBean("action2",1,"岳阳");

        ResponseBean responseBean = ResponseBean.ok(result);
        String jsonStr = JSON.toJSONString(responseBean);
        String objectJsonStr = MAPPER.writeValueAsString(responseBean);

        System.out.println(jsonStr);
        System.out.println(objectJsonStr);

        ResponseBean responseBean1 = JSON.parseObject(jsonStr,ResponseBean.class);
        ResponseBean responseBean2 = MAPPER.readValue(jsonStr,ResponseBean.class);

        System.out.println(responseBean1);

    }

}

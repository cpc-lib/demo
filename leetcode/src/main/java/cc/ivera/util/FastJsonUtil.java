package cc.ivera.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * JSON 转换
 */
public class FastJsonUtil {

    private static final Logger log = LoggerFactory.getLogger(FastJsonUtil.class);

    /**
     * 把Java对象转换成json字符串
     *
     * @param object 待转化为JSON字符串的Java对象
     * @return json 串 or null
     */
    public static String parseObjToJson(Object object) {
        String string = null;
        try {
            string = JSONObject.toJSONString(object);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return string;
    }

    /**
     * 将Json字符串信息转换成对应的Java对象
     *
     * @param json json字符串对象
     * @param c    对应的类型
     */
    public static <T> T parseJsonToObj(String json, Class<T> c) {
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            return JSON.toJavaObject(jsonObject, c);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public static <T> List<T> parseJsonArrToList(String jsonArray, Class<T> clazz) {
        try {
            Type type = new TypeReference<List<T>>(clazz) {
            }.getType();
            return JSON.parseObject(jsonArray, type);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Collections.emptyList();
    }

}

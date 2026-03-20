package cc.ivera.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;


public class GsonUtil {

    private static final Logger log = LoggerFactory.getLogger(GsonUtil.class);
    private static final Gson filterNullGson;
    private static final Gson nullableGson;
    private static final Gson gson;

    static {
        nullableGson = new GsonBuilder().enableComplexMapKeySerialization().serializeNulls().setDateFormat("yyyy-MM-dd HH:mm:ss:SSS").create();
        filterNullGson = new GsonBuilder().enableComplexMapKeySerialization().setDateFormat("yyyy-MM-dd HH:mm:ss:SSS").create();
        gson = new GsonBuilder().create();
    }

    /**
     * 根据对象返回json   不过滤空值字段
     */
    public static String toJsonWtihNullField(Object obj) {
        return nullableGson.toJson(obj);
    }


    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static String toJsonV1(Object obj) {
        return filterNullGson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromJsonV1(String json, Type type) {
        return nullableGson.fromJson(json, type);
    }


    public static <T> List<T> parseJsonArrToList(String jsonArray, Class<T> clazz) {
        try {
            List<T> ts = gson.fromJson(jsonArray, TypeToken.getParameterized(List.class, clazz).getType());
            return ts;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Collections.emptyList();
    }

}

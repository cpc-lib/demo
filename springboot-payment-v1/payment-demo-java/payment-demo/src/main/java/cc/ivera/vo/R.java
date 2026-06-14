package cc.ivera.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class R<T> {

    private Integer code; //响应码
    private String message; //响应消息
    private T data; //响应数据

    public static R<Map<String, Object>> ok() {
        R<Map<String, Object>> r = new R<>();
        r.setCode(0);
        r.setMessage("成功");
        r.setData(new HashMap<String, Object>());
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMessage("成功");
        r.setData(data);
        return r;
    }

    public static R<Map<String, Object>> error() {
        R<Map<String, Object>> r = new R<>();
        r.setCode(-1);
        r.setMessage("失败");
        r.setData(new HashMap<String, Object>());
        return r;
    }

    public static <T> R<T> error(String message) {
        R<T> r = new R<>();
        r.setCode(-1);
        r.setMessage(message);
        return r;
    }

    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> data(String key, Object value) {
        Map<String, Object> map;
        if (this.data instanceof Map) {
            map = (Map<String, Object>) this.data;
        } else {
            map = new HashMap<>();
            this.data = (T) map;
        }
        map.put(key, value);
        return (R<Map<String, Object>>) this;
    }

}

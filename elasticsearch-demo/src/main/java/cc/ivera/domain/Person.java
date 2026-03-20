package cc.ivera.domain;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person {
    /**
     * 忽略进行此字段
     */
    @JSONField(serialize = false)
    private String id;
    private String name;
    private Integer age;
    private String address;
}

package cc.ivera.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测试实体类
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Body implements Serializable {
    private String name;
    private int age;

}

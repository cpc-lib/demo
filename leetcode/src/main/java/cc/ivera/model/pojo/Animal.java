package cc.ivera.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 12/4/2021 10:34 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Animal {
    public String location;
    protected int age;
    int length;
    private int height;
}

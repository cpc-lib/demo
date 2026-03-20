package cc.ivera.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import cc.ivera.enums.FoodType;

import java.io.Serializable;

/**
 * @author e2607
 * @version 1.0
 * @description: 菜品信息
 * @date 11/29/2021 11:59 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dish implements Serializable {
    private String name;
    private Boolean flag;
    private Integer price;
    private FoodType type;
}

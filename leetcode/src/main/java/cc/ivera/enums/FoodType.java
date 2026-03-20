package cc.ivera.enums;

/**
 * @author e2607
 * @version 1.0
 * @description: 菜品类别枚举
 * @date 11/30/2021 12:02 AM
 */
public enum FoodType {

    MEAT("meat"),
    FISH("fish"),
    OTHER("other");

    private String type;

    FoodType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }


}

package cc.ivera.enums;

import com.baomidou.mybatisplus.core.enums.IEnum;

public enum GenderEnum implements IEnum<Integer> {

    Male(1,"男"),
    Female(2,"女");

    private int value;
    private String desc;

    GenderEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.desc;
    }
}
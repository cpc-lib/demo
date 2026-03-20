package cc.ivera.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import java.util.Objects;

public enum StateEnum implements IEnum<Integer> {

    ENABLE(1,"正常"),
    DISABLE(2,"停用");

    private Integer value;
    private String comment;

    StateEnum(Integer value,String commment){
        this.value=value;
        this.comment=comment;
    }

    public String getComment(){
        return this.comment;
    }


    public static StateEnum getByCode(Integer code) {
        for (StateEnum value : StateEnum.values()) {
            if (Objects.equals(code, value.getValue())) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "StateEnum{" +
                "value=" + value +
                ", comment='" + comment + '\'' +
                '}';
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

}

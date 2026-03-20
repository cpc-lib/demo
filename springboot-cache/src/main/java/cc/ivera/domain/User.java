package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import cc.ivera.enums.GenderEnum;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private Long id;
    private String userName;

    // 插入数据时进行填充
    @TableField(select = false, fill = FieldFill.INSERT) //查询时不返回该字段的值
    private String password;
    private String name;
    private Integer age;

    @TableField(value = "email") //指定数据表中字段名
    private String mail;

    @TableField(exist = false)
    private String address; //在数据库表中是不存在的

    @Version //乐观锁的版本字段
    private Integer version;

    @TableLogic // 逻辑删除字段 ，1-删除，0-未删除
    private Integer deleted;

    private GenderEnum gender; //性别，枚举类型
}

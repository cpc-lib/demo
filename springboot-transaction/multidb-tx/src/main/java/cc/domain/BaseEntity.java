package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public abstract class BaseEntity implements Serializable {

    /**
     * 使用mybatisplus的来实现表单字段的填充，仅填充一次
     */
    @TableField(fill= FieldFill.INSERT)
    private Date created;

    /**
     * 使用mybatisplus的来实现表单字段的填充，新建与更新时使用
     */
    @TableField(fill= FieldFill.INSERT_UPDATE)
    private Date updated;

}

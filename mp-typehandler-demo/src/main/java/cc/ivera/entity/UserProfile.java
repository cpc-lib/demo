package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cc.ivera.typehandler.ListStringTypeHandler;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * List<String> -> MySQL JSON
     */
    @TableField(typeHandler = ListStringTypeHandler.class)
    private List<String> hobbies;

    /**
     * List<TagItem> -> MySQL JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TagItem> tags;
}

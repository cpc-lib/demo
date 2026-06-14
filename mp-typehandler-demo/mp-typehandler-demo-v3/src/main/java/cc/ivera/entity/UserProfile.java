package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import cc.ivera.audit.annotation.LogField;
import cc.ivera.typehandler.ListStringTypeHandler;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    @LogField(label = "姓名")
    private String name;

    /**
     * List<String> -> MySQL JSON
     */
    @LogField(label = "爱好")
    @TableField(typeHandler = ListStringTypeHandler.class)
    private List<String> hobbies;

    /**
     * List<TagItem> -> MySQL JSON
     */
    @LogField(label = "标签")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TagItem> tags;
}

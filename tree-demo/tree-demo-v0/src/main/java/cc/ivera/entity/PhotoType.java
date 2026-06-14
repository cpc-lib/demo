
package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("base_photo_type")
public class PhotoType {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String parentId;
    private Integer layer;
    private String name;
    private String nameEn;
    private Integer sort;
    private String nameRule;
    private String nameRuleEn;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

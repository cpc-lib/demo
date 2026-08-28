package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_resource_permission_tag")
public class RagResourcePermissionTag {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("permission_tag")
    private String permissionTag;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("is_deleted")
    private Integer isDeleted;
}

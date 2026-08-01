package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_role")
public class SysUserRole {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("user_id")
    private String userId;
    @TableField("role_id")
    private Long roleId;
    @TableField("role_code")
    private String roleCode;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("is_deleted")
    private Integer isDeleted;
}

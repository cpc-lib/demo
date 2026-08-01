package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("external_user_id")
    private String externalUserId;
    @TableField("username")
    private String username;
    @TableField("display_name")
    private String displayName;
    @TableField("email")
    private String email;
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;
    @TableField("password_updated_at")
    private LocalDateTime passwordUpdatedAt;
    @TableField("must_change_password")
    private Integer mustChangePassword;
    @TableField("status")
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @TableField("is_deleted")
    private Integer isDeleted;
}

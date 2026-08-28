package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_platform_admin")
public class SysPlatformAdmin {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("singleton_key")
    private Integer singletonKey;
    @TableField("admin_username")
    private String adminUsername;
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

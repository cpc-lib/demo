package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_admin_impersonation_session")
public class SysAdminImpersonationSession {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("session_no")
    private String sessionNo;
    @TableField("operator_user_id")
    private String operatorUserId;
    @TableField("operator_tenant_id")
    private Long operatorTenantId;
    @TableField("target_tenant_id")
    private Long targetTenantId;
    @TableField("impersonation_reason")
    private String impersonationReason;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("revoked_at")
    private LocalDateTime revokedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
}

package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_audit_log")
public class SysOperationAuditLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("operator_user_id")
    private String operatorUserId;
    @TableField("operator_tenant_id")
    private Long operatorTenantId;
    @TableField("target_tenant_id")
    private Long targetTenantId;
    @TableField("impersonation_reason")
    private String impersonationReason;
    @TableField("request_id")
    private String requestId;
    @TableField("source_ip")
    private String sourceIp;
    @TableField("operation")
    private String operation;
    @TableField("resource_type")
    private String resourceType;
    @TableField("resource_id")
    private String resourceId;
    @TableField("result")
    private String result;
    @TableField("detail_json")
    private String detailJson;
    @TableField("created_at")
    private LocalDateTime createdAt;
}

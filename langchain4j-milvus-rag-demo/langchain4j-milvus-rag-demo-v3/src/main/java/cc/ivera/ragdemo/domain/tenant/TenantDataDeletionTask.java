package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant_data_deletion_task")
public class TenantDataDeletionTask {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("task_no")
    private String taskNo;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("requested_by")
    private String requestedBy;
    @TableField("reason")
    private String reason;
    @TableField("execution_mode")
    private String executionMode;
    @TableField("task_status")
    private String taskStatus;
    @TableField("lock_owner")
    private String lockOwner;
    @TableField("lock_until")
    private LocalDateTime lockUntil;
    @TableField("verify_result_json")
    private String verifyResultJson;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
    @TableField("error_message")
    private String errorMessage;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_ingestion_task")
public class RagIngestionTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("document_version_id")
    private Long documentVersionId;

    @TableField("task_no")
    private String taskNo;

    @TableField("task_type")
    private String taskType;

    @TableField("task_status")
    private Integer taskStatus;

    @TableField("progress")
    private Integer progress;

    @TableField("current_stage")
    private String currentStage;

    @TableField("stage_progress")
    private Integer stageProgress;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("failed_count")
    private Integer failedCount;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    @TableField("cancel_requested")
    private Boolean cancelRequested;

    @TableField("cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @TableField("cancel_requested_by")
    private String cancelRequestedBy;

    @TableField("partial_success")
    private Boolean partialSuccess;

    @TableField("last_event_id")
    private Long lastEventId;

    @TableField("heartbeat_at")
    private LocalDateTime heartbeatAt;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("trace_id")
    private String traceId;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("lock_version")
    private Long lockVersion;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

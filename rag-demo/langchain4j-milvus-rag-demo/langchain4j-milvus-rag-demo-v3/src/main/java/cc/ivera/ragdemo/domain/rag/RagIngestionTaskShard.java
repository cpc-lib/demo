package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_ingestion_task_shard")
public class RagIngestionTaskShard {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("task_id")
    private Long taskId;

    @TableField("stage_code")
    private String stageCode;

    @TableField("document_id")
    private Long documentId;

    @TableField("document_version_id")
    private Long documentVersionId;

    @TableField("shard_key")
    private String shardKey;

    @TableField("shard_type")
    private String shardType;

    @TableField("shard_index")
    private Integer shardIndex;

    @TableField("shard_status")
    private String shardStatus;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("input_hash")
    private String inputHash;

    @TableField("output_ref")
    private String outputRef;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

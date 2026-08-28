package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_ingestion_task_event")
public class RagIngestionTaskEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("task_id")
    private Long taskId;

    @TableField("event_type")
    private String eventType;

    @TableField("stage_code")
    private String stageCode;

    @TableField("shard_key")
    private String shardKey;

    @TableField("progress")
    private Integer progress;

    @TableField("stage_progress")
    private Integer stageProgress;

    @TableField("message")
    private String message;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_feedback_revision_task")
public class RagFeedbackRevisionTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("revision_no")
    private String revisionNo;

    @TableField("feedback_id")
    private Long feedbackId;

    @TableField("query_log_id")
    private Long queryLogId;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("chunk_uid")
    private String chunkUid;

    @TableField("revision_type")
    private String revisionType;

    @TableField("revision_status")
    private String revisionStatus;

    @TableField("before_snapshot_json")
    private String beforeSnapshotJson;

    @TableField("after_snapshot_json")
    private String afterSnapshotJson;

    @TableField("expected_fix")
    private String expectedFix;

    @TableField("verification_query")
    private String verificationQuery;

    @TableField("verification_result_json")
    private String verificationResultJson;

    @TableField("created_by")
    private String createdBy;

    @TableField("assignee")
    private String assignee;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_keyword_reindex_job")
public class RagKeywordReindexJob {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("job_no")
    private String jobNo;
    @TableField("source_index")
    private String sourceIndex;
    @TableField("target_index")
    private String targetIndex;
    @TableField("alias_name")
    private String aliasName;
    @TableField("template_version")
    private String templateVersion;
    @TableField("job_status")
    private String jobStatus;
    @TableField("progress")
    private Integer progress;
    @TableField("total_count")
    private Long totalCount;
    @TableField("success_count")
    private Long successCount;
    @TableField("failed_count")
    private Long failedCount;
    @TableField("sample_validation_json")
    private String sampleValidationJson;
    @TableField("rollback_target")
    private String rollbackTarget;
    @TableField("error_message")
    private String errorMessage;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

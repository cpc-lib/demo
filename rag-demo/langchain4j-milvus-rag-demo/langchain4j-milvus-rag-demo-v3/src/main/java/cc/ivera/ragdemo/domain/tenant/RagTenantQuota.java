package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_tenant_quota")
public class RagTenantQuota {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("max_documents")
    private Long maxDocuments;
    @TableField("max_storage_bytes")
    private Long maxStorageBytes;
    @TableField("max_file_bytes")
    private Long maxFileBytes;
    @TableField("daily_ocr_limit")
    private Long dailyOcrLimit;
    @TableField("daily_embedding_tokens")
    private Long dailyEmbeddingTokens;
    @TableField("max_concurrent_ingestion_tasks")
    private Long maxConcurrentIngestionTasks;
    @TableField("daily_query_limit")
    private Long dailyQueryLimit;
    @TableField("monthly_budget_cents")
    private Long monthlyBudgetCents;
    @TableField("enabled")
    private Boolean enabled;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

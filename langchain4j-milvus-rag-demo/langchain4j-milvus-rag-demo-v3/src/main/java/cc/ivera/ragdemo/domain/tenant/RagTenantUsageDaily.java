package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rag_tenant_usage_daily")
public class RagTenantUsageDaily {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("usage_date")
    private LocalDate usageDate;
    @TableField("document_count")
    private Long documentCount;
    @TableField("storage_bytes")
    private Long storageBytes;
    @TableField("ocr_count")
    private Long ocrCount;
    @TableField("embedding_tokens")
    private Long embeddingTokens;
    @TableField("vector_count")
    private Long vectorCount;
    @TableField("query_count")
    private Long queryCount;
    @TableField("llm_tokens")
    private Long llmTokens;
    @TableField("estimated_cost_cents")
    private Long estimatedCostCents;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

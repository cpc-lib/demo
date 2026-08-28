package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_query_log_archive")
public class RagQueryLogArchive {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("source_query_log_id")
    private Long sourceQueryLogId;

    @TableField("delete_no")
    private String deleteNo;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("trace_id")
    private String traceId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("query_type")
    private String queryType;

    @TableField("query_text")
    private String queryText;

    @TableField("retrieval_mode")
    private String retrievalMode;

    @TableField("knowledge_base_ids_json")
    private String knowledgeBaseIdsJson;

    @TableField("top_k")
    private Integer topK;

    @TableField("min_score")
    private Double minScore;

    @TableField("content_types_json")
    private String contentTypesJson;

    @TableField("permission_tags_json")
    private String permissionTagsJson;

    @TableField("prompt_text")
    private String promptText;

    @TableField("answer_text")
    private String answerText;

    @TableField("knowledge_hit")
    private Boolean knowledgeHit;

    @TableField("hit_count")
    private Integer hitCount;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("llm_provider")
    private String llmProvider;

    @TableField("llm_model")
    private String llmModel;

    @TableField("embedding_provider")
    private String embeddingProvider;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("estimated_input_cost")
    private BigDecimal estimatedInputCost;

    @TableField("estimated_output_cost")
    private BigDecimal estimatedOutputCost;

    @TableField("estimated_embedding_cost")
    private BigDecimal estimatedEmbeddingCost;

    @TableField("estimated_total_cost")
    private BigDecimal estimatedTotalCost;

    @TableField("cost_currency")
    private String costCurrency;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("status")
    private String status;

    @TableField("archive_status")
    private String archiveStatus;

    @TableField("retention_until")
    private LocalDateTime retentionUntil;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("query_created_at")
    private LocalDateTime queryCreatedAt;

    @TableField("query_updated_at")
    private LocalDateTime queryUpdatedAt;

    @TableField("archived_at")
    private LocalDateTime archivedAt;
}

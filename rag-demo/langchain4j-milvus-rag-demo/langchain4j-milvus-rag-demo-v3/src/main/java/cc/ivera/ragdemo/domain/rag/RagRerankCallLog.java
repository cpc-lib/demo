package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_rerank_call_log")
public class RagRerankCallLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("provider")
    private String provider;

    @TableField("model")
    private String model;

    @TableField("query_hash")
    private String queryHash;

    @TableField("api_key_hash")
    private String apiKeyHash;

    @TableField("tenant_external_id")
    private String tenantExternalId;

    @TableField("request_window")
    private LocalDateTime requestWindow;

    @TableField("candidate_count")
    private Integer candidateCount;

    @TableField("top_k")
    private Integer topK;

    @TableField("input_tokens")
    private Integer inputTokens;

    @TableField("output_tokens")
    private Integer outputTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("success")
    private Boolean success;

    @TableField("fallback")
    private Boolean fallback;

    @TableField("estimated_cost")
    private BigDecimal estimatedCost;

    @TableField("error_code")
    private String errorCode;

    @TableField("http_status")
    private Integer httpStatus;

    @TableField("error_code_normalized")
    private String errorCodeNormalized;

    @TableField("degraded_reason")
    private String degradedReason;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("cache_hit")
    private Boolean cacheHit;

    @TableField("error_message")
    private String errorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

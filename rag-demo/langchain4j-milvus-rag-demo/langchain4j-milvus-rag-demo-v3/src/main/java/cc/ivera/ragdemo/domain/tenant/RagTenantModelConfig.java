package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_tenant_model_config")
public class RagTenantModelConfig {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("provider")
    private String provider;
    @TableField("model_type")
    private String modelType;
    @TableField("model_name")
    private String modelName;
    @TableField("base_url")
    private String baseUrl;
    @TableField("api_key_secret_ref")
    private String apiKeySecretRef;
    @TableField("temperature")
    private BigDecimal temperature;
    @TableField("dimension")
    private Integer dimension;
    @TableField("image_size")
    private String imageSize;
    @TableField("image_quality")
    private String imageQuality;
    @TableField("poll_interval_millis")
    private Integer pollIntervalMillis;
    @TableField("rate_limit_qps")
    private Integer rateLimitQps;
    @TableField("monthly_budget_cents")
    private Long monthlyBudgetCents;
    
    /** 超时时间（秒） */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;
    
    /** 最大重试次数 */
    @TableField("max_retries")
    private Integer maxRetries;
    
    /** 最大输出Token数 */
    @TableField("max_tokens")
    private Integer maxTokens;
    
    /** 多样性惩罚因子 */
    @TableField("frequency_penalty")
    private BigDecimal frequencyPenalty;
    
    /** 出现惩罚因子 */
    @TableField("presence_penalty")
    private BigDecimal presencePenalty;
    
    /** 采样概率阈值 */
    @TableField("top_p")
    private BigDecimal topP;
    
    @TableField("enabled")
    private Boolean enabled;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}

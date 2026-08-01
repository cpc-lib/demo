package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 请求 DTO：创建或更新模型配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigUpsertRequest {

    /**
     * 模型类型：LLM、EMBEDDING 或 IMAGE
     */
    @NotBlank(message = "模型类型不能为空")
    @Pattern(regexp = "^(LLM|EMBEDDING|IMAGE)$", message = "模型类型必须是 LLM、EMBEDDING 或 IMAGE")
    private String modelType;

    /**
     * 模型提供商，默认为 openai-compatible
     */
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "提供商名称只能包含字母、数字、下划线和连字符")
    @Size(max = 50, message = "提供商名称长度不能超过50个字符")
    private String provider = "openai-compatible";

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    /**
     * API Base URL
     */
    @Size(max = 500, message = "Base URL 长度不能超过500个字符")
    private String baseUrl;

    /**
     * API Key 或密钥引用。
     */
    @Size(max = 2048, message = "API Key 或密钥引用长度不能超过2048个字符")
    private String apiKeySecretRef;

    /**
     * 温度参数，范围 0-2
     */
    @DecimalMin(value = "0", message = "Temperature 最小值为0")
    @DecimalMax(value = "2", message = "Temperature 最大值为2")
    private BigDecimal temperature;

    /**
     * Embedding 向量维度
     */
    @Min(value = 1, message = "维度必须大于0")
    @Max(value = 16384, message = "维度不能超过16384")
    private Integer dimension;

    /**
     * 文生图输出尺寸，例如 1024x1024。
     */
    @Size(max = 64, message = "图片尺寸长度不能超过64个字符")
    @Pattern(regexp = "^[0-9]{2,5}[xX*][0-9]{2,5}$", message = "图片尺寸格式必须类似 1024x1024")
    private String imageSize;

    /**
     * 文生图质量参数，例如 standard 或 hd。
     */
    @Size(max = 64, message = "图片质量参数长度不能超过64个字符")
    private String imageQuality;

    /**
     * 文生图异步任务轮询间隔（毫秒）。
     */
    @Min(value = 500, message = "轮询间隔不能小于500毫秒")
    @Max(value = 60000, message = "轮询间隔不能超过60000毫秒")
    private Integer pollIntervalMillis;

    /**
     * QPS 限流
     */
    @Min(value = 1, message = "QPS 限流必须大于0")
    @Max(value = 10000, message = "QPS 限流不能超过10000")
    private Integer rateLimitQps;

    /**
     * 月度预算（美分）
     */
    @Min(value = 0, message = "月度预算不能为负数")
    private Long monthlyBudgetCents;

    /**
     * 保存后是否启用该模型配置。
     * true 会停用当前租户同类型其它配置；false 只保存为候选配置。
     */
    private Boolean enabled;
    
    /**
     * 超时时间（秒）
     */
    @Min(value = 1, message = "超时时间必须大于0")
    @Max(value = 600, message = "超时时间不能超过600秒")
    private Integer timeoutSeconds;
    
    /**
     * 最大重试次数
     */
    @Min(value = 0, message = "最大重试次数不能为负数")
    @Max(value = 10, message = "最大重试次数不能超过10")
    private Integer maxRetries;
    
    /**
     * 最大输出Token数
     */
    @Min(value = 1, message = "最大Token数必须大于0")
    @Max(value = 32768, message = "最大Token数不能超过32768")
    private Integer maxTokens;
    
    /**
     * 多样性惩罚因子，范围 -2.0 到 2.0
     */
    @DecimalMin(value = "-2", message = "frequencyPenalty 最小值为-2")
    @DecimalMax(value = "2", message = "frequencyPenalty 最大值为2")
    private BigDecimal frequencyPenalty;
    
    /**
     * 出现惩罚因子，范围 -2.0 到 2.0
     */
    @DecimalMin(value = "-2", message = "presencePenalty 最小值为-2")
    @DecimalMax(value = "2", message = "presencePenalty 最大值为2")
    private BigDecimal presencePenalty;
    
    /**
     * 采样概率阈值，范围 0 到 1
     */
    @DecimalMin(value = "0", message = "topP 最小值为0")
    @DecimalMax(value = "1", message = "topP 最大值为1")
    private BigDecimal topP;
}

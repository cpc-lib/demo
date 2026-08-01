package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_model_pricing")
public class RagModelPricing {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("provider")
    private String provider;

    @TableField("model")
    private String model;

    @TableField("input_cost_per_1k_tokens")
    private BigDecimal inputCostPer1kTokens;

    @TableField("output_cost_per_1k_tokens")
    private BigDecimal outputCostPer1kTokens;

    @TableField("currency")
    private String currency;

    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    @TableField("effective_to")
    private LocalDateTime effectiveTo;

    @TableField("enabled")
    private Boolean enabled;
}

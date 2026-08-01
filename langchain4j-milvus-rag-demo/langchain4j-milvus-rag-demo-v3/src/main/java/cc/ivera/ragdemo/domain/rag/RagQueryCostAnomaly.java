package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_query_cost_anomaly")
public class RagQueryCostAnomaly {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("anomaly_type")
    private String anomalyType;

    @TableField("severity")
    private String severity;

    @TableField("metric_name")
    private String metricName;

    @TableField("metric_value")
    private BigDecimal metricValue;

    @TableField("baseline_value")
    private BigDecimal baselineValue;

    @TableField("window_start")
    private LocalDateTime windowStart;

    @TableField("window_end")
    private LocalDateTime windowEnd;

    @TableField("status")
    private String status;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

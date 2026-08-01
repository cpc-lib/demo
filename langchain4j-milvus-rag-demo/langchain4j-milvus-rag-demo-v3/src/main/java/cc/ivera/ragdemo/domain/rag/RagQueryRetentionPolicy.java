package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_query_retention_policy")
public class RagQueryRetentionPolicy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("policy_name")
    private String policyName;

    @TableField("query_type")
    private String queryType;

    @TableField("status_filter")
    private String statusFilter;

    @TableField("retention_days")
    private Integer retentionDays;

    @TableField("archive_before_delete")
    private Boolean archiveBeforeDelete;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

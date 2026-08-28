package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_query_log_delete_audit")
public class RagQueryLogDeleteAudit {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("delete_no")
    private String deleteNo;

    @TableField("operator")
    private String operator;

    @TableField("delete_mode")
    private String deleteMode;

    @TableField("reason")
    private String reason;

    @TableField("query_log_ids_json")
    private String queryLogIdsJson;

    @TableField("matched_count")
    private Integer matchedCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("failed_count")
    private Integer failedCount;

    @TableField("filter_json")
    private String filterJson;

    @TableField("result_json")
    private String resultJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

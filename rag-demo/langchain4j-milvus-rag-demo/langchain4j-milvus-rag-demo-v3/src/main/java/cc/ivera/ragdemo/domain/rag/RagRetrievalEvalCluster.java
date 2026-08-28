package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_eval_cluster")
public class RagRetrievalEvalCluster {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("run_id")
    private Long runId;

    @TableField("cluster_key")
    private String clusterKey;

    @TableField("cluster_label")
    private String clusterLabel;

    @TableField("failure_type")
    private String failureType;

    @TableField("case_count")
    private Integer caseCount;

    @TableField("sample_case_ids_json")
    private String sampleCaseIdsJson;

    @TableField("suggestion")
    private String suggestion;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

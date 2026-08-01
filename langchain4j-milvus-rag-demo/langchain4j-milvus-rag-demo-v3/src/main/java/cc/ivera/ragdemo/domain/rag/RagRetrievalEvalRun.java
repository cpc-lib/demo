package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_eval_run")
public class RagRetrievalEvalRun {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("run_no")
    private String runNo;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("version_tag")
    private String versionTag;

    @TableField("retrieval_mode")
    private String retrievalMode;

    @TableField("total_cases")
    private Integer totalCases;

    @TableField("hit_rate")
    private Double hitRate;

    @TableField("mean_reciprocal_rank")
    private Double meanReciprocalRank;

    @TableField("mean_recall")
    private Double meanRecall;

    @TableField("source")
    private String source;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

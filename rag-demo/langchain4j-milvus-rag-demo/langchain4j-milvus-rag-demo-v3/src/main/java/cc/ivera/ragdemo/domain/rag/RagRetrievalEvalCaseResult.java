package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_eval_case_result")
public class RagRetrievalEvalCaseResult {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("run_id")
    private Long runId;

    @TableField("case_db_id")
    private Long caseDbId;

    @TableField("case_id")
    private String caseId;

    @TableField("query_text")
    private String queryText;

    @TableField("top_k")
    private Integer topK;

    @TableField("expected_chunk_ids_json")
    private String expectedChunkIdsJson;

    @TableField("retrieved_chunk_ids_json")
    private String retrievedChunkIdsJson;

    @TableField("hit")
    private Boolean hit;

    @TableField("reciprocal_rank")
    private Double reciprocalRank;

    @TableField("recall_value")
    private Double recall;

    @TableField("failure_type")
    private String failureType;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("retrieval_trace_json")
    private String retrievalTraceJson;

    @TableField("cluster_key")
    private String clusterKey;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

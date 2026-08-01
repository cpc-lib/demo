package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_retrieval_eval_case")
public class RagRetrievalEvalCase {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("version_tag")
    private String versionTag;

    @TableField("case_id")
    private String caseId;

    @TableField("query_text")
    private String queryText;

    @TableField("retrieval_mode")
    private String retrievalMode;

    @TableField("query_category")
    private String queryCategory;

    @TableField("difficulty_level")
    private String difficultyLevel;

    @TableField("language")
    private String language;

    @TableField("expected_answer_type")
    private String expectedAnswerType;

    @TableField("top_k")
    private Integer topK;

    @TableField("min_score")
    private Double minScore;

    @TableField("content_types_json")
    private String contentTypesJson;

    @TableField("permission_tags_json")
    private String permissionTagsJson;

    @TableField("expected_chunk_ids_json")
    private String expectedChunkIdsJson;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Integer isDeleted;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_base")
public class RagKnowledgeBase {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("kb_code")
    private String kbCode;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("vector_store_type")
    private String vectorStoreType;

    @TableField("vector_collection")
    private String vectorCollection;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    @TableField("chunk_strategy")
    private String chunkStrategy;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    @TableField("retrieval_top_k")
    private Integer retrievalTopK;

    @TableField("min_score")
    private BigDecimal minScore;

    @TableField("config_json")
    private String configJson;

    @TableField("status")
    private Integer status;

    @TableField("lock_version")
    private Long lockVersion;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Integer isDeleted;
}

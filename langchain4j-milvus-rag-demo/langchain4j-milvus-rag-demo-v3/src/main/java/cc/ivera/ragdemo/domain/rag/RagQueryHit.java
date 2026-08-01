package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_query_hit")
public class RagQueryHit {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("query_log_id")
    private Long queryLogId;

    @TableField("rank_no")
    private Integer rankNo;

    @TableField("score")
    private Double score;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private String documentId;

    @TableField("document_name")
    private String documentName;

    @TableField("chunk_id")
    private String chunkId;

    @TableField("chunk_version")
    private Integer chunkVersion;

    @TableField("content_type")
    private String contentType;

    @TableField("modality")
    private String modality;

    @TableField("retrieval_source")
    private String retrievalSource;

    @TableField("image_asset_id")
    private Long imageAssetId;

    @TableField("fusion_score")
    private Double fusionScore;

    @TableField("page_no")
    private Integer pageNo;

    @TableField("section_title")
    private String sectionTitle;

    @TableField("image_url")
    private String imageUrl;

    @TableField("content_snippet")
    private String contentSnippet;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

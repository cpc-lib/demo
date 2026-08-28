package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document_chunk")
public class RagDocumentChunk {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("source_document_id")
    private String sourceDocumentId;

    @TableField("document_version_id")
    private Long documentVersionId;

    @TableField("chunk_uid")
    private String chunkUid;

    @TableField("chunk_version")
    private Integer chunkVersion;

    @TableField("chunk_status")
    private String chunkStatus;

    @TableField("is_current")
    private Boolean currentFlag;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("parent_chunk_id")
    private Long parentChunkId;

    @TableField("parent_chunk_uid")
    private String parentChunkUid;

    @TableField("source")
    private String source;

    @TableField("file_name")
    private String fileName;

    @TableField("content_type")
    private String contentType;

    @TableField("page_start")
    private Integer pageStart;

    @TableField("page_end")
    private Integer pageEnd;

    @TableField("char_start")
    private Integer charStart;

    @TableField("char_end")
    private Integer charEnd;

    @TableField("title")
    private String title;

    @TableField("section_path")
    private String sectionPath;

    @TableField("image_url")
    private String imageUrl;

    @TableField("image_caption")
    private String imageCaption;

    @TableField("image_number")
    private String imageNumber;

    @TableField("permission_tags")
    private String permissionTags;

    @TableField("tenant_external_id")
    private String tenantExternalId;

    @TableField("content")
    private String content;

    @TableField("content_summary")
    private String contentSummary;

    @TableField("content_hash")
    private String contentHash;

    @TableField("character_count")
    private Integer characterCount;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("vector_store_type")
    private String vectorStoreType;

    @TableField("vector_collection")
    private String vectorCollection;

    @TableField("milvus_alias")
    private String milvusAlias;

    @TableField("vector_id")
    private String vectorId;

    @TableField("text_vector_ids")
    private String textVectorIds;

    @TableField("image_vector_ids")
    private String imageVectorIds;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    @TableField("embedding_status")
    private Integer embeddingStatus;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Integer isDeleted;
}

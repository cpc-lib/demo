package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document")
public class RagDocument {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_uid")
    private String documentUid;

    @TableField("document_name")
    private String documentName;

    @TableField("source_type")
    private Integer sourceType;

    @TableField("source_uri")
    private String sourceUri;

    @TableField("object_key")
    private String objectKey;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("file_extension")
    private String fileExtension;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_hash")
    private String fileHash;

    @TableField("current_version_id")
    private Long currentVersionId;

    @TableField("current_version_no")
    private Integer currentVersionNo;

    @TableField("page_count")
    private Integer pageCount;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("character_count")
    private Long characterCount;

    @TableField("token_count")
    private Long tokenCount;

    @TableField("parse_status")
    private Integer parseStatus;

    @TableField("chunk_status")
    private Integer chunkStatus;

    @TableField("embedding_status")
    private Integer embeddingStatus;

    @TableField("document_status")
    private Integer documentStatus;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Integer isDeleted;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_document_version")
public class RagDocumentVersion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("version_uid")
    private String versionUid;

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

    @TableField("version_status")
    private Integer versionStatus;

    @TableField("is_current")
    private Boolean currentFlag;

    @TableField("version_note")
    private String versionNote;

    @TableField("approval_status")
    private String approvalStatus;

    @TableField("approval_comment")
    private String approvalComment;

    @TableField("approved_by")
    private String approvedBy;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

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

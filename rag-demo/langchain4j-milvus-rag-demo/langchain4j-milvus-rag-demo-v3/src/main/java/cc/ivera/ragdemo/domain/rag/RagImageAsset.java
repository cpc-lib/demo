package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_image_asset")
public class RagImageAsset {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("document_id")
    private Long documentId;

    @TableField("document_version_id")
    private Long documentVersionId;

    @TableField("source_document_id")
    private String sourceDocumentId;

    @TableField("image_id")
    private String imageId;

    @TableField("chunk_uid")
    private String chunkUid;

    @TableField("content_type")
    private String contentType;

    @TableField("asset_path")
    private String assetPath;

    @TableField("image_url")
    private String imageUrl;

    @TableField("page_no")
    private Integer pageNo;

    @TableField("coordinate_json")
    private String coordinateJson;

    @TableField("section_title")
    private String sectionTitle;

    @TableField("image_caption")
    private String imageCaption;

    @TableField("image_number")
    private String imageNumber;

    @TableField("ocr_text")
    private String ocrText;

    @TableField("ocr_status")
    private String ocrStatus;

    @TableField("ocr_confidence")
    private Double ocrConfidence;

    @TableField("ocr_provider")
    private String ocrProvider;

    @TableField("ocr_model")
    private String ocrModel;

    @TableField("ocr_error_message")
    private String ocrErrorMessage;

    @TableField("visual_status")
    private String visualStatus;

    @TableField("visual_schema_valid")
    private Boolean visualSchemaValid;

    @TableField("visual_confidence")
    private Double visualConfidence;

    @TableField("visual_json")
    private String visualJson;

    @TableField("visual_schema_errors")
    private String visualSchemaErrors;

    @TableField("text_vector_ids")
    private String textVectorIds;

    @TableField("image_vector_ids")
    private String imageVectorIds;

    @TableField("image_embedding_status")
    private String imageEmbeddingStatus;

    @TableField("image_embedding_model")
    private String imageEmbeddingModel;

    @TableField("image_embedding_dimension")
    private Integer imageEmbeddingDimension;

    @TableField("image_embedding_error_message")
    private String imageEmbeddingErrorMessage;

    @TableField("image_embedding_updated_at")
    private LocalDateTime imageEmbeddingUpdatedAt;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("reviewed_by")
    private String reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("review_updated_visual_json")
    private String reviewUpdatedVisualJson;

    @TableField("review_updated_ocr_text")
    private String reviewUpdatedOcrText;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Integer isDeleted;
}

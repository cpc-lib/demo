package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagImageAsset;
import cc.ivera.ragdemo.mapper.RagImageAssetMapper;
import cc.ivera.ragdemo.model.knowledge.*;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.ingest.OcrTextExtractor;
import cc.ivera.ragdemo.service.ingest.VisionEmbeddingClient;
import cc.ivera.ragdemo.service.ingest.VisionStructuredAnalysisClient;
import cc.ivera.ragdemo.service.ragops.ImageAssetReviewPolicy;
import cc.ivera.ragdemo.service.ragops.VisualStructuredContentValidator;
import cc.ivera.ragdemo.service.ragops.VisualValidationResult;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagImageAssetService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 200;

    private final RagImageAssetMapper imageAssetMapper;
    private final ObjectMapper objectMapper;
    private final ImageAssetReviewPolicy reviewPolicy = new ImageAssetReviewPolicy();
    private RagProperties ragProperties = new RagProperties();
    private OcrTextExtractor ocrTextExtractor;
    private VisionStructuredAnalysisClient visionStructuredAnalysisClient;
    private VisualStructuredContentValidator visualStructuredContentValidator;
    private VisionEmbeddingClient visionEmbeddingClient;
    private MultimodalVectorStore multimodalVectorStore;

    @Autowired(required = false)
    public void setRagProperties(RagProperties ragProperties) {
        if (ragProperties != null) {
            this.ragProperties = ragProperties;
        }
    }

    @Autowired(required = false)
    public void setOcrTextExtractor(OcrTextExtractor ocrTextExtractor) {
        this.ocrTextExtractor = ocrTextExtractor;
    }

    @Autowired(required = false)
    public void setVisionStructuredAnalysisClient(VisionStructuredAnalysisClient visionStructuredAnalysisClient) {
        this.visionStructuredAnalysisClient = visionStructuredAnalysisClient;
    }

    @Autowired(required = false)
    public void setVisualStructuredContentValidator(VisualStructuredContentValidator visualStructuredContentValidator) {
        this.visualStructuredContentValidator = visualStructuredContentValidator;
    }

    @Autowired(required = false)
    public void setVisionEmbeddingClient(VisionEmbeddingClient visionEmbeddingClient) {
        this.visionEmbeddingClient = visionEmbeddingClient;
    }

    @Autowired(required = false)
    public void setMultimodalVectorStore(MultimodalVectorStore multimodalVectorStore) {
        this.multimodalVectorStore = multimodalVectorStore;
    }

    public RagImageAsset upsertFromExtractedImage(ExtractedImageKnowledge image,
                                                  KnowledgeChunkRecord chunk,
                                                  VisualValidationResult validation,
                                                  Map<String, Object> ingestionMetadata) {
        String imageId = image.id();
        Long tenantId = effectiveTenantId(parseLongOrDefault(value(ingestionMetadata, "tenant_id"), 0L));
        Long knowledgeBaseId = parseLongOrDefault(value(ingestionMetadata, "knowledge_base_id"), 0L);
        RagImageAsset existing = imageAssetMapper.selectOne(new LambdaQueryWrapper<RagImageAsset>()
                .eq(RagImageAsset::getTenantId, tenantId)
                .eq(RagImageAsset::getImageId, imageId)
                .last("LIMIT 1"));

        RagImageAsset asset = existing == null ? new RagImageAsset() : existing;
        asset.setTenantId(tenantId);
        asset.setKnowledgeBaseId(knowledgeBaseId);
        asset.setDocumentId(parseLong(value(ingestionMetadata, "document_db_id")));
        asset.setDocumentVersionId(parseLong(value(ingestionMetadata, "document_version_id")));
        asset.setSourceDocumentId(chunk.documentId());
        asset.setImageId(imageId);
        asset.setChunkUid(chunk.chunkId());
        asset.setContentType(chunk.contentType());
        asset.setAssetPath(image.assetPath() == null ? null : image.assetPath().toString());
        asset.setImageUrl(image.imageUrl());
        asset.setPageNo(image.pageNo());
        asset.setCoordinateJson(coordinateJson(image.coordinate()));
        asset.setSectionTitle(image.sectionTitle());
        asset.setImageCaption(image.imageCaption());
        asset.setImageNumber(image.imageNumber());
        asset.setOcrText(image.ocrText());
        asset.setOcrStatus(valueOrDefault(value(ingestionMetadata, "ocr_status"), asset.getOcrStatus()));
        asset.setOcrConfidence(parseDouble(value(ingestionMetadata, "ocr_confidence")));
        asset.setOcrProvider(valueOrDefault(value(ingestionMetadata, "ocr_provider"), asset.getOcrProvider()));
        asset.setOcrModel(valueOrDefault(value(ingestionMetadata, "ocr_model"), asset.getOcrModel()));
        asset.setOcrErrorMessage(valueOrDefault(value(ingestionMetadata, "ocr_error_message"), asset.getOcrErrorMessage()));
        asset.setVisualStatus(validation.status());
        asset.setVisualSchemaValid(validation.schemaValid());
        asset.setVisualConfidence(validation.confidence());
        asset.setVisualJson(validation.normalizedJson());
        asset.setVisualSchemaErrors(valueOrDefault(validation.schemaErrors(), value(ingestionMetadata, "visual_schema_errors")));
        asset.setTextVectorIds(toJson(chunk.textVectorIds()));
        asset.setImageVectorIds(toJson(chunk.imageVectorIds()));
        asset.setImageEmbeddingStatus(valueOrDefault(value(ingestionMetadata, "image_embedding_status"), asset.getImageEmbeddingStatus()));
        asset.setImageEmbeddingModel(valueOrDefault(value(ingestionMetadata, "image_embedding_model"), asset.getImageEmbeddingModel()));
        asset.setImageEmbeddingDimension(parseInteger(value(ingestionMetadata, "image_embedding_dimension")));
        asset.setImageEmbeddingErrorMessage(valueOrDefault(value(ingestionMetadata, "image_embedding_error_message"), asset.getImageEmbeddingErrorMessage()));
        asset.setImageEmbeddingUpdatedAt(parseDateTime(value(ingestionMetadata, "image_embedding_updated_at")));
        asset.setReviewStatus(valueOrDefault(value(ingestionMetadata, "review_status"), defaultReviewStatus(asset)));
        asset.setIsDeleted(0);
        asset.setUpdatedAt(LocalDateTime.now());
        if (asset.getCreatedAt() == null) {
            asset.setCreatedAt(LocalDateTime.now());
        }

        if (existing == null) {
            imageAssetMapper.insert(asset);
        } else {
            imageAssetMapper.updateById(asset);
        }
        return asset;
    }

    public List<RagImageAsset> listAssets(Long tenantId,
                                          Long knowledgeBaseId,
                                          String sourceDocumentId,
                                          String contentType,
                                          String visualStatus,
                                          String reviewStatus,
                                          String ocrStatus,
                                          String imageEmbeddingStatus,
                                          Double minConfidence,
                                          Integer limit) {
        return pageAssets(
                tenantId,
                knowledgeBaseId,
                sourceDocumentId,
                contentType,
                visualStatus,
                reviewStatus,
                ocrStatus,
                imageEmbeddingStatus,
                minConfidence,
                PageQuery.of(1, limit, DEFAULT_PAGE_SIZE, "updatedAt", "DESC", MAX_PAGE_SIZE)
        ).records();
    }

    public List<RagImageAsset> listAssets(Long tenantId,
                                          Long knowledgeBaseId,
                                          String sourceDocumentId,
                                          String contentType,
                                          String visualStatus,
                                          Double minConfidence,
                                          Integer limit) {
        return listAssets(tenantId, knowledgeBaseId, sourceDocumentId, contentType, visualStatus,
                null, null, null, minConfidence, limit);
    }

    public PageResponse<RagImageAsset> pageAssets(Long tenantId,
                                                  Long knowledgeBaseId,
                                                  String sourceDocumentId,
                                                  String contentType,
                                                  String visualStatus,
                                                  String reviewStatus,
                                                  String ocrStatus,
                                                  String imageEmbeddingStatus,
                                                  Double minConfidence,
                                                  PageQuery pageQuery) {
        PageQuery query = normalizePageQuery(pageQuery);
        Long effectiveTenantId = effectiveTenantId(tenantId);
        long total = imageAssetMapper.selectCount(queryWrapper(
                effectiveTenantId,
                knowledgeBaseId,
                sourceDocumentId,
                contentType,
                visualStatus,
                reviewStatus,
                ocrStatus,
                imageEmbeddingStatus,
                minConfidence
        ));
        LambdaQueryWrapper<RagImageAsset> rowsQuery = queryWrapper(
                effectiveTenantId,
                knowledgeBaseId,
                sourceDocumentId,
                contentType,
                visualStatus,
                reviewStatus,
                ocrStatus,
                imageEmbeddingStatus,
                minConfidence
        );
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, imageAssetMapper.selectList(rowsQuery));
    }

    public PageResponse<RagImageAsset> pageAssets(Long tenantId,
                                                  Long knowledgeBaseId,
                                                  String sourceDocumentId,
                                                  String contentType,
                                                  String visualStatus,
                                                  Double minConfidence,
                                                  PageQuery pageQuery) {
        return pageAssets(tenantId, knowledgeBaseId, sourceDocumentId, contentType, visualStatus,
                null, null, null, minConfidence, pageQuery);
    }

    public PageResponse<RagImageAsset> pageReviewPending(Long tenantId,
                                                         Long knowledgeBaseId,
                                                         PageQuery pageQuery) {
        return pageAssets(tenantId, knowledgeBaseId, null, null, null,
                ImageAssetReviewPolicy.REVIEW_PENDING, null, null, null, pageQuery);
    }

    private LambdaQueryWrapper<RagImageAsset> queryWrapper(Long tenantId,
                                                          Long knowledgeBaseId,
                                                          String sourceDocumentId,
                                                          String contentType,
                                                          String visualStatus,
                                                          String reviewStatus,
                                                          String ocrStatus,
                                                          String imageEmbeddingStatus,
                                                          Double minConfidence) {
        LambdaQueryWrapper<RagImageAsset> query = new LambdaQueryWrapper<RagImageAsset>()
                .eq(RagImageAsset::getIsDeleted, 0);
        if (tenantId != null) {
            query.eq(RagImageAsset::getTenantId, tenantId);
        }
        if (knowledgeBaseId != null) {
            query.eq(RagImageAsset::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (StringUtils.hasText(sourceDocumentId)) {
            query.eq(RagImageAsset::getSourceDocumentId, sourceDocumentId.trim());
        }
        if (StringUtils.hasText(contentType)) {
            query.eq(RagImageAsset::getContentType, contentType.trim().toLowerCase());
        }
        if (StringUtils.hasText(visualStatus)) {
            query.eq(RagImageAsset::getVisualStatus, visualStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(reviewStatus)) {
            query.eq(RagImageAsset::getReviewStatus, reviewStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(ocrStatus)) {
            query.eq(RagImageAsset::getOcrStatus, ocrStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(imageEmbeddingStatus)) {
            query.eq(RagImageAsset::getImageEmbeddingStatus, imageEmbeddingStatus.trim().toUpperCase());
        }
        if (minConfidence != null) {
            query.ge(RagImageAsset::getVisualConfidence, minConfidence);
        }
        return query;
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery) {
        PageQuery query = pageQuery == null
                ? PageQuery.of(1, null, DEFAULT_PAGE_SIZE, "updatedAt", "DESC", MAX_PAGE_SIZE)
                : pageQuery;
        return query.withDefaultSort("updatedAt", "DESC");
    }

    private void applyOrder(LambdaQueryWrapper<RagImageAsset> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagImageAsset::getId);
            case "createdAt" -> wrapper.orderBy(true, asc, RagImageAsset::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagImageAsset::getUpdatedAt);
            case "visualConfidence" -> wrapper.orderBy(true, asc, RagImageAsset::getVisualConfidence);
            case "pageNo" -> wrapper.orderBy(true, asc, RagImageAsset::getPageNo);
            case "visualStatus" -> wrapper.orderBy(true, asc, RagImageAsset::getVisualStatus);
            case "reviewStatus" -> wrapper.orderBy(true, asc, RagImageAsset::getReviewStatus);
            case "ocrStatus" -> wrapper.orderBy(true, asc, RagImageAsset::getOcrStatus);
            case "imageEmbeddingStatus" -> wrapper.orderBy(true, asc, RagImageAsset::getImageEmbeddingStatus);
            case "imageEmbeddingUpdatedAt" -> wrapper.orderBy(true, asc, RagImageAsset::getImageEmbeddingUpdatedAt);
            default -> wrapper.orderByDesc(RagImageAsset::getUpdatedAt);
        }
    }

    public RagImageAsset getRequired(Long id) {
        RagImageAsset asset = imageAssetMapper.selectById(id);
        if (asset == null || Integer.valueOf(1).equals(asset.getIsDeleted())) {
            throw new IllegalArgumentException("Image asset not found: " + id);
        }
        TenantContextHolder.currentTenantId().ifPresent(currentTenant -> {
            if (!currentTenant.equals(asset.getTenantId())) {
                throw new cc.ivera.ragdemo.exception.TenantAccessDeniedException("Image asset belongs to another tenant");
            }
        });
        return asset;
    }

    public RagImageAsset approve(Long id, ImageAssetReviewRequest request) {
        RagImageAsset asset = getRequired(id);
        asset.setReviewStatus(reviewPolicy.approve(asset.getReviewStatus()));
        applyReviewFields(asset, request);
        imageAssetMapper.updateById(asset);
        return asset;
    }

    public RagImageAsset reject(Long id, ImageAssetReviewRequest request) {
        RagImageAsset asset = getRequired(id);
        asset.setReviewStatus(reviewPolicy.reject(asset.getReviewStatus()));
        applyReviewFields(asset, request);
        imageAssetMapper.updateById(asset);
        return asset;
    }

    public RagImageAsset updateReview(Long id, ImageAssetReviewRequest request) {
        RagImageAsset asset = getRequired(id);
        if (request != null && StringUtils.hasText(request.updatedVisualJson())) {
            VisualValidationResult validation = validateVisualJson(request.updatedVisualJson());
            asset.setReviewUpdatedVisualJson(validation.normalizedJson());
            asset.setVisualSchemaValid(validation.schemaValid());
            asset.setVisualConfidence(validation.confidence());
            asset.setVisualSchemaErrors(validation.schemaErrors());
        }
        if (request != null && request.updatedOcrText() != null) {
            asset.setReviewUpdatedOcrText(request.updatedOcrText());
        }
        asset.setReviewStatus(reviewPolicy.update(asset.getReviewStatus()));
        applyReviewFields(asset, request);
        imageAssetMapper.updateById(asset);
        return asset;
    }

    public RagImageAsset reprocess(Long id, ImageAssetReprocessRequest request) {
        RagImageAsset asset = getRequired(id);
        boolean runOcr = request == null || request.ocr() == null || request.ocr();
        boolean runVision = request == null || request.visionAnalysis() == null || request.visionAnalysis();
        boolean runEmbedding = request == null || request.imageEmbedding() == null || request.imageEmbedding();
        ExtractedImageKnowledge image = toExtractedImage(asset);

        OcrExtractionResult ocrResult = null;
        if (runOcr) {
            require(ocrTextExtractor, "OCR extractor is not available");
            ocrResult = ocrTextExtractor.extractResult(image);
            asset.setOcrText(firstNonBlank(ocrResult.text(), asset.getOcrText()));
            asset.setOcrStatus(ocrResult.status());
            asset.setOcrConfidence(ocrResult.confidence());
            asset.setOcrProvider(ocrResult.provider());
            asset.setOcrModel(ocrResult.model());
            asset.setOcrErrorMessage(ocrResult.errorMessage());
            image = toExtractedImage(asset);
        }

        VisualValidationResult validation = null;
        if (runVision) {
            require(visionStructuredAnalysisClient, "Vision analysis client is not available");
            require(visualStructuredContentValidator, "Visual structured content validator is not available");
            validation = visualStructuredContentValidator.validate(visionStructuredAnalysisClient.analyze(image));
            asset.setVisualStatus(validation.status());
            asset.setVisualSchemaValid(validation.schemaValid());
            asset.setVisualConfidence(validation.confidence());
            asset.setVisualJson(validation.normalizedJson());
            asset.setVisualSchemaErrors(validation.schemaErrors());
        }

        VisionEmbeddingResult embedding = null;
        if (runEmbedding) {
            require(visionEmbeddingClient, "Vision embedding client is not available");
            embedding = visionEmbeddingClient.embed(new ImageEmbeddingRequest(
                    asset.getImageId(),
                    assetPath(asset),
                    asset.getImageUrl(),
                    null,
                    null,
                    ragProperties.getMultimodalIngest().getVisionEmbeddingDimension()
            ));
            asset.setImageEmbeddingStatus(embedding.status());
            asset.setImageEmbeddingModel(embedding.model());
            asset.setImageEmbeddingDimension(embedding.dimension());
            asset.setImageEmbeddingErrorMessage(embedding.errorMessage());
            asset.setImageEmbeddingUpdatedAt(LocalDateTime.now());
            if (embedding.success() && multimodalVectorStore != null && multimodalVectorStore.enabled()) {
                String vectorId = firstVectorId(asset);
                multimodalVectorStore.upsert(new MultimodalVectorRecord(
                        vectorId,
                        asset.getChunkUid(),
                        asset.getImageId(),
                        asset.getDocumentId(),
                        asset.getDocumentVersionId(),
                        asset.getKnowledgeBaseId(),
                        asset.getTenantId(),
                        asset.getContentType(),
                        "image",
                        List.of(),
                        embedding.vector(),
                        embedding.model(),
                        embedding.dimension(),
                        asset.getPageNo(),
                        asset.getSectionTitle(),
                        "",
                        reviewStatus(asset, validation, ocrResult, embedding),
                        true,
                        System.currentTimeMillis()
                ));
                asset.setImageVectorIds(toJson(List.of(vectorId)));
            }
        }

        asset.setReviewStatus(reviewStatus(asset, validation, ocrResult, embedding));
        if (request != null && StringUtils.hasText(request.operator())) {
            asset.setReviewedBy(request.operator().trim());
            asset.setReviewedAt(LocalDateTime.now());
        }
        asset.setUpdatedAt(LocalDateTime.now());
        imageAssetMapper.updateById(asset);
        return asset;
    }

    private void applyReviewFields(RagImageAsset asset, ImageAssetReviewRequest request) {
        if (request != null) {
            if (StringUtils.hasText(request.comment())) {
                asset.setReviewComment(request.comment().trim());
            }
            if (StringUtils.hasText(request.operator())) {
                asset.setReviewedBy(request.operator().trim());
            }
        }
        asset.setReviewedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
    }

    private VisualValidationResult validateVisualJson(String visualJson) {
        if (visualStructuredContentValidator == null) {
            return new VisualValidationResult("SUCCESS", true, 1.0D, visualJson, "[]");
        }
        return visualStructuredContentValidator.validate(visualJson);
    }

    private ExtractedImageKnowledge toExtractedImage(RagImageAsset asset) {
        return new ExtractedImageKnowledge(
                asset.getImageId(),
                contentType(asset.getContentType()),
                assetPath(asset),
                asset.getImageUrl(),
                asset.getPageNo(),
                null,
                asset.getSectionTitle(),
                asset.getImageCaption(),
                asset.getImageNumber(),
                null,
                null,
                firstNonBlank(asset.getReviewUpdatedOcrText(), asset.getOcrText()),
                firstNonBlank(asset.getReviewUpdatedVisualJson(), asset.getVisualJson())
        );
    }

    private ContentType contentType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ContentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("Invalid content type value: {}", value);
            return null;
        }
    }

    private Path assetPath(RagImageAsset asset) {
        if (asset == null || !StringUtils.hasText(asset.getAssetPath())) {
            return null;
        }
        return Path.of(asset.getAssetPath());
    }

    private String firstVectorId(RagImageAsset asset) {
        List<String> ids = parseStringList(asset.getImageVectorIds());
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        String chunkUid = StringUtils.hasText(asset.getChunkUid()) ? asset.getChunkUid() : asset.getImageId();
        return chunkUid + ":image:v" + (asset.getId() == null ? "reprocess" : asset.getId());
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("Failed to parse string list JSON, returning empty list: {}", e.getMessage());
            return List.of();
        }
    }

    private String reviewStatus(RagImageAsset asset,
                                VisualValidationResult validation,
                                OcrExtractionResult ocrResult,
                                VisionEmbeddingResult embedding) {
        Boolean schemaValid = validation == null ? asset.getVisualSchemaValid() : validation.schemaValid();
        Double visualConfidence = validation == null ? asset.getVisualConfidence() : validation.confidence();
        Double ocrConfidence = ocrResult == null ? asset.getOcrConfidence() : ocrResult.confidence();
        String embeddingStatus = embedding == null ? asset.getImageEmbeddingStatus() : embedding.status();
        return reviewPolicy.initialStatus(
                Boolean.TRUE.equals(schemaValid),
                visualConfidence,
                ocrConfidence,
                embeddingStatus,
                ragProperties.getMultimodalIngest().getLowConfidenceThreshold()
        );
    }

    private String defaultReviewStatus(RagImageAsset asset) {
        return reviewPolicy.initialStatus(
                Boolean.TRUE.equals(asset.getVisualSchemaValid()),
                asset.getVisualConfidence(),
                asset.getOcrConfidence(),
                asset.getImageEmbeddingStatus(),
                ragProperties.getMultimodalIngest().getLowConfidenceThreshold()
        );
    }

    private void require(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private String coordinateJson(PageCoordinate coordinate) {
        if (coordinate == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("x", coordinate.x());
        value.put("y", coordinate.y());
        value.put("width", coordinate.width());
        value.put("height", coordinate.height());
        value.put("pageWidth", coordinate.pageWidth());
        value.put("pageHeight", coordinate.pageHeight());
        return toJson(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize image asset metadata", e);
        }
    }

    private String value(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        Long parsed = parseLong(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Long effectiveTenantId(Long requestTenantId) {
        return TenantContextHolder.currentTenantId().orElse(requestTenantId);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Invalid long value: {}", value);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Invalid integer value: {}", value);
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Invalid double value: {}", value);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            log.debug("Invalid datetime value: {}", value);
            return null;
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}

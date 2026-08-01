package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.*;
import cc.ivera.ragdemo.service.ingest.*;
import cc.ivera.ragdemo.service.ragops.ImageAssetReviewPolicy;
import cc.ivera.ragdemo.service.ragops.VisualStructuredContentValidator;
import cc.ivera.ragdemo.service.ragops.VisualValidationResult;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static cc.ivera.ragdemo.service.ragops.IngestionStageProgressPolicy.*;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KnowledgeIngestionService {

    private static final int EMBED_BATCH_SIZE = 10;

    private final TikaParser tikaParser;
    private final MultimodalDocumentParser multimodalDocumentParser;
    private final VisionStructuredAnalysisClient visionStructuredAnalysisClient;
    private final OcrTextExtractor ocrTextExtractor;
    private final VisualStructuredContentValidator visualStructuredContentValidator;
    private final Splitter splitter;
    private final EmbeddingModel embeddingModel;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final KnowledgeChunkManagementService chunkManagementService;
    private final RagImageAssetService imageAssetService;
    private final ObjectMapper objectMapper;
    private final VisionEmbeddingClient visionEmbeddingClient;
    private final MultimodalVectorStore multimodalVectorStore;
    private final RagProperties ragProperties;
    private final ImageAssetReviewPolicy imageAssetReviewPolicy;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeToMap(Object metadataObj) {
        if (metadataObj == null) {
            return Collections.emptyMap();
        }
        if (metadataObj instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        try {
            Method toMap = metadataObj.getClass().getMethod("toMap");
            Object res = toMap.invoke(metadataObj);
            if (res instanceof Map<?, ?> m) {
                Map<String, Object> out = new HashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
                return out;
            }
        } catch (Exception ignore) {
        }
        return Collections.emptyMap();
    }

    public KnowledgeIngestionResult ingestFileBytes(String fileName, byte[] bytes, Map<String, Object> extraMetadata) {
        return ingestFileBytes(fileName, bytes, extraMetadata, KnowledgeIngestionProgressListener.NOOP);
    }

    public KnowledgeIngestionResult ingestFileBytes(String fileName,
                                                    byte[] bytes,
                                                    Map<String, Object> extraMetadata,
                                                    KnowledgeIngestionProgressListener progressListener) {
        KnowledgeIngestionProgressListener listener = progressListener == null
                ? KnowledgeIngestionProgressListener.NOOP
                : progressListener;
        try {
            String safeFileName = Optional.ofNullable(fileName).orElse("unknown");
            String documentId = String.valueOf(extraMetadata.getOrDefault(
                    "document_id",
                    MultimodalDocumentParser.stableDocumentId(safeFileName, bytes)
            ));
            listener.checkCancelled();
            listener.stageStarted(PARSE_DOCUMENT);
            ParsedKnowledgeDocument parsed = multimodalDocumentParser.parse(new java.io.ByteArrayInputStream(bytes), safeFileName, documentId);
            listener.stageCompleted(PARSE_DOCUMENT, 1, 0, 1);
            if (parsed.images() == null || parsed.images().isEmpty()) {
                listener.stageSkipped(EXTRACT_ASSETS, "No image or table assets extracted");
                listener.stageSkipped(OCR_IMAGES, "No image assets");
                listener.stageSkipped(VISION_ANALYSIS, "No image assets");
                listener.stageSkipped(EMBED_IMAGE, "No image assets");
            } else {
                listener.stageStarted(EXTRACT_ASSETS);
                listener.stageCompleted(EXTRACT_ASSETS, parsed.images().size(), 0, parsed.images().size());
                if (!imageEmbeddingConfigured()) {
                    listener.stageSkipped(EMBED_IMAGE, "Image vector embedding is disabled");
                }
            }

            Map<String, Object> metadata = new HashMap<>(extraMetadata);
            metadata.put("source", metadata.getOrDefault("source", "file"));
            metadata.put("fileName", metadata.getOrDefault("fileName", safeFileName));
            metadata.put("document_id", documentId);
            metadata.put("content_type", metadata.getOrDefault("content_type", "text"));

            KnowledgeIngestionResult textResult = ingestDocument(parsed.textDocument(), metadata, listener);
            List<KnowledgeChunkRecord> records = new ArrayList<>(textResult.chunkRecords());
            List<KnowledgeChunkRecord> imageRecords = ingestImages(parsed.images(), safeFileName, documentId, metadata, listener);
            records.addAll(imageRecords);
            return new KnowledgeIngestionResult(textResult.chunks() + imageRecords.size(), records);
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest file bytes: " + e.getMessage(), e);
        }
    }

    private KnowledgeIngestionResult ingestDocument(Document document, Map<String, Object> extraMetadata) {
        return ingestDocument(document, extraMetadata, KnowledgeIngestionProgressListener.NOOP);
    }

    private KnowledgeIngestionResult ingestDocument(Document document,
                                                    Map<String, Object> extraMetadata,
                                                    KnowledgeIngestionProgressListener listener) {
        listener.checkCancelled();
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        EmbeddingStore<TextSegment> embeddingStore = activeMilvus.store();

        Map<String, Object> merged = new HashMap<>(safeToMap(document.metadata()));
        merged.putAll(extraMetadata);
        merged.putIfAbsent("content_type", "text");
        merged.putIfAbsent("document_id", "doc-" + UUID.randomUUID());
        merged.put("milvusAlias", activeMilvus.alias());
        merged.put("milvusCollection", activeMilvus.config().getCollection());

        Document docWithMeta = Document.from(document.text(), Metadata.from(merged));
        listener.stageStarted(SPLIT_CHUNKS);
        List<TextSegment> segments = splitter.split(docWithMeta);
        listener.stageCompleted(SPLIT_CHUNKS, segments.size(), 0, segments.size());
        List<KnowledgeChunkRecord> records = new ArrayList<>();
        String documentId = String.valueOf(merged.get("document_id"));
        String contentType = String.valueOf(merged.get("content_type"));
        String source = stringValue(merged.get("source"));
        String fileName = stringValue(merged.get("fileName"));

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String chunkId = documentId + "-text-" + i;
            KnowledgeChunkRecord record = KnowledgeChunkRecord.builder()
                    .chunkId(chunkId)
                    .documentId(documentId)
                    .source(source)
                    .fileName(fileName)
                    .contentType(contentType)
                    .textContent(segment.text())
                    .permissionTags(List.of())
                    .version(chunkManagementService.nextVersion(chunkId))
                    .status(ChunkStatus.ACTIVE)
                    .current(true)
                    .milvusAlias(activeMilvus.alias())
                    .milvusCollection(activeMilvus.config().getCollection())
                    .metadataJson("{}")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Map<String, Object> metadata = new HashMap<>(safeToMap(segment.metadata()));
            metadata.putAll(merged);
            metadata.putAll(chunkManagementService.metadataFor(record));
            metadata.put("chunk_index", i);
            metadata.put("chunkId", String.valueOf(i));
            removeNullValues(metadata);

            segments.set(i, TextSegment.from(segment.text(), Metadata.from(metadata)));
            records.add(record);
        }

        int embeddedCount = 0;
        int vectorCount = 0;
        listener.stageStarted(EMBED_TEXT);
        listener.stageStarted(WRITE_VECTOR);
        for (int from = 0; from < segments.size(); from += EMBED_BATCH_SIZE) {
            listener.checkCancelled();
            int to = Math.min(from + EMBED_BATCH_SIZE, segments.size());
            List<TextSegment> batchSegments = new ArrayList<>(segments.subList(from, to));
            Response<List<Embedding>> response = embeddingModel.embedAll(batchSegments);
            embeddedCount += batchSegments.size();
            listener.stageProgress(EMBED_TEXT, embeddedCount, 0, segments.size());
            listener.checkCancelled();
            List<String> vectorIds = embeddingStore.addAll(response.content(), batchSegments);
            vectorCount += vectorIds.size();
            listener.stageProgress(WRITE_VECTOR, vectorCount, 0, segments.size());
            for (int i = 0; i < vectorIds.size(); i++) {
                KnowledgeChunkRecord embedded = records.get(from + i)
                        .toBuilder()
                        .textVectorIds(List.of(vectorIds.get(i)))
                        .build();
                records.set(from + i, chunkManagementService.registerEmbeddedActiveChunk(embedded));
            }
        }
        listener.stageCompleted(EMBED_TEXT, embeddedCount, 0, segments.size());
        listener.stageCompleted(WRITE_VECTOR, vectorCount, 0, segments.size());

        return new KnowledgeIngestionResult(segments.size(), records);
    }

    private List<KnowledgeChunkRecord> ingestImages(List<ExtractedImageKnowledge> images,
                                                    String fileName,
                                                    String documentId,
                                                    Map<String, Object> ingestionMetadata,
                                                    KnowledgeIngestionProgressListener listener) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        EmbeddingStore<TextSegment> embeddingStore = activeMilvus.store();
        List<TextSegment> imageSegments = new ArrayList<>();
        List<KnowledgeChunkRecord> records = new ArrayList<>();
        List<ExtractedImageKnowledge> enrichedImages = new ArrayList<>();
        List<VisualValidationResult> validationResults = new ArrayList<>();
        List<OcrExtractionResult> ocrResults = new ArrayList<>();

        listener.stageStarted(OCR_IMAGES);
        listener.stageStarted(VISION_ANALYSIS);
        for (int i = 0; i < images.size(); i++) {
            listener.checkCancelled();
            ExtractedImageKnowledge image = images.get(i);
            OcrExtractionResult ocrResult = ocrTextExtractor.extractResult(image);
            String ocrText = firstText(ocrResult.text(), image.ocrText());
            listener.stageProgress(OCR_IMAGES, i + 1, 0, images.size());
            ExtractedImageKnowledge imageWithOcr = new ExtractedImageKnowledge(
                    image.id(),
                    image.contentType(),
                    image.assetPath(),
                    image.imageUrl(),
                    image.pageNo(),
                    image.coordinate(),
                    image.sectionTitle(),
                    image.imageCaption(),
                    image.imageNumber(),
                    image.previousText(),
                    image.nextText(),
                    ocrText,
                    image.visualStructuredContent()
            );
            listener.checkCancelled();
            String visualJson = visionStructuredAnalysisClient.analyze(imageWithOcr);
            VisualValidationResult validation = visualStructuredContentValidator.validate(visualJson);
            listener.stageProgress(VISION_ANALYSIS, i + 1, 0, images.size());
            ExtractedImageKnowledge enriched = new ExtractedImageKnowledge(
                    imageWithOcr.id(),
                    imageWithOcr.contentType(),
                    imageWithOcr.assetPath(),
                    imageWithOcr.imageUrl(),
                    imageWithOcr.pageNo(),
                    imageWithOcr.coordinate(),
                    imageWithOcr.sectionTitle(),
                    imageWithOcr.imageCaption(),
                    imageWithOcr.imageNumber(),
                    imageWithOcr.previousText(),
                    imageWithOcr.nextText(),
                    imageWithOcr.ocrText(),
                    validation.normalizedJson()
            );
            String chunkId = enriched.id();
            KnowledgeChunkRecord record = KnowledgeChunkRecord.builder()
                    .chunkId(chunkId)
                    .documentId(documentId)
                    .source("file")
                    .fileName(fileName)
                    .contentType(contentTypeValue(enriched))
                    .textContent(enriched.searchableText())
                    .imageVectorIds(List.of())
                    .imageUrl(enriched.imageUrl())
                    .pageNo(enriched.pageNo())
                    .sectionTitle(enriched.sectionTitle())
                    .imageCaption(enriched.imageCaption())
                    .imageNumber(enriched.imageNumber())
                    .permissionTags(List.of())
                    .tenantId(stringValue(ingestionMetadata.getOrDefault("tenant_id", "")))
                    .version(chunkManagementService.nextVersion(chunkId))
                    .status(ChunkStatus.ACTIVE)
                    .current(true)
                    .milvusAlias(activeMilvus.alias())
                    .milvusCollection(activeMilvus.config().getCollection())
                    .metadataJson(metadataJson(enriched, validation))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.putAll(ingestionMetadata);
            metadata.put("source", "file");
            metadata.put("fileName", fileName);
            metadata.put("document_id", documentId);
            metadata.put("content_type", contentTypeValue(enriched));
            metadata.put("image_id", enriched.id());
            metadata.put("image_url", enriched.imageUrl());
            metadata.put("page_no", enriched.pageNo());
            metadata.put("section_title", enriched.sectionTitle());
            metadata.put("image_caption", enriched.imageCaption());
            metadata.put("image_number", enriched.imageNumber());
            metadata.put("permission_tags", "");
            metadata.put("tenant_id", stringValue(ingestionMetadata.getOrDefault("tenant_id", "")));
            metadata.put("coordinate_json", coordinateJson(enriched));
            metadata.put("ocr_text", enriched.ocrText());
            metadata.put("visual_status", validation.status());
            metadata.put("visual_schema_valid", validation.schemaValid());
            metadata.put("visual_confidence", validation.confidence());
            metadata.put("metadata_json", metadataJson(enriched, validation));
            metadata.put("chunkId", "image-" + i);
            metadata.put("milvusAlias", activeMilvus.alias());
            metadata.put("milvusCollection", activeMilvus.config().getCollection());
            metadata.putAll(chunkManagementService.metadataFor(record));
            metadata.put("chunk_index", "image-" + i);
            removeNullValues(metadata);

            imageSegments.add(TextSegment.from(enriched.searchableText(), Metadata.from(metadata)));
            records.add(record);
            enrichedImages.add(enriched);
            validationResults.add(validation);
            ocrResults.add(ocrResult);
        }
        listener.stageCompleted(OCR_IMAGES, images.size(), 0, images.size());
        listener.stageCompleted(VISION_ANALYSIS, images.size(), 0, images.size());

        int vectorCount = 0;
        listener.stageStarted(WRITE_VECTOR);
        if (imageEmbeddingConfigured()) {
            listener.stageStarted(EMBED_IMAGE);
        }
        for (int from = 0; from < imageSegments.size(); from += EMBED_BATCH_SIZE) {
            listener.checkCancelled();
            int to = Math.min(from + EMBED_BATCH_SIZE, imageSegments.size());
            List<TextSegment> batchSegments = new ArrayList<>(imageSegments.subList(from, to));
            Response<List<Embedding>> response = embeddingModel.embedAll(batchSegments);
            listener.checkCancelled();
            List<String> vectorIds = embeddingStore.addAll(response.content(), batchSegments);
            vectorCount += vectorIds.size();
            listener.stageProgress(WRITE_VECTOR, vectorCount, 0, imageSegments.size());
            for (int i = 0; i < vectorIds.size(); i++) {
                int recordIndex = from + i;
                KnowledgeChunkRecord embedded = records.get(recordIndex)
                        .toBuilder()
                        .textVectorIds(List.of(vectorIds.get(i)))
                        .build();
                KnowledgeChunkRecord registered = chunkManagementService.registerEmbeddedActiveChunk(embedded);
                VisionEmbeddingResult imageEmbedding = embedImageIfEnabled(
                        enrichedImages.get(recordIndex),
                        registered,
                        validationResults.get(recordIndex),
                        ocrResults.get(recordIndex),
                        vectorAsList(response.content().get(i)),
                        ingestionMetadata
                );
                if (imageEmbedding.vectorId() != null) {
                    registered = chunkManagementService.registerEmbeddedActiveChunk(registered.toBuilder()
                            .imageVectorIds(List.of(imageEmbedding.vectorId()))
                            .build());
                }
                records.set(recordIndex, registered);
                imageAssetService.upsertFromExtractedImage(
                        enrichedImages.get(recordIndex),
                        registered,
                        validationResults.get(recordIndex),
                        imageAssetMetadata(ingestionMetadata, ocrResults.get(recordIndex), imageEmbedding, validationResults.get(recordIndex))
                );
                if (imageEmbeddingConfigured()) {
                    listener.stageProgress(EMBED_IMAGE, recordIndex + 1, 0, imageSegments.size());
                }
            }
        }
        listener.stageCompleted(WRITE_VECTOR, vectorCount, 0, imageSegments.size());
        if (imageEmbeddingConfigured()) {
            listener.stageCompleted(EMBED_IMAGE, imageSegments.size(), 0, imageSegments.size());
        }
        return records;
    }

    private VisionEmbeddingResult embedImageIfEnabled(ExtractedImageKnowledge image,
                                                      KnowledgeChunkRecord chunk,
                                                      VisualValidationResult validation,
                                                      OcrExtractionResult ocrResult,
                                                      List<Float> textVector,
                                                      Map<String, Object> ingestionMetadata) {
        RagProperties.MultimodalIngest config = ragProperties.getMultimodalIngest();
        String provider = stringValue(config.getVisionEmbeddingProvider());
        String model = stringValue(config.getVisionEmbeddingModel());
        if (!imageEmbeddingConfigured()) {
            return VisionEmbeddingResult.skipped(provider, model);
        }
        VisionEmbeddingResult embedding = visionEmbeddingClient.embed(new ImageEmbeddingRequest(
                image.id(),
                image.assetPath(),
                image.imageUrl(),
                null,
                null,
                config.getVisionEmbeddingDimension()
        ));
        if (!embedding.success()) {
            return embedding;
        }
        String vectorId = chunk.chunkId() + ":image:v" + chunk.version();
        String reviewStatus = reviewStatus(validation, ocrResult, embedding);
        multimodalVectorStore.upsert(new MultimodalVectorRecord(
                vectorId,
                chunk.chunkId(),
                image.id(),
                parseLong(value(ingestionMetadata, "document_db_id")),
                parseLong(value(ingestionMetadata, "document_version_id")),
                parseLong(value(ingestionMetadata, "knowledge_base_id")),
                parseLongOrDefault(value(ingestionMetadata, "tenant_id"), 0L),
                contentTypeValue(image),
                "image",
                textVector,
                embedding.vector(),
                embedding.model(),
                embedding.dimension(),
                image.pageNo(),
                image.sectionTitle(),
                String.join(",", chunk.permissionTags() == null ? List.of() : chunk.permissionTags()),
                reviewStatus,
                chunk.current(),
                Instant.now().toEpochMilli()
        ));
        return new VisionEmbeddingResult(
                embedding.status(),
                embedding.vector(),
                vectorId,
                embedding.provider(),
                embedding.model(),
                embedding.dimension(),
                embedding.inputTokens(),
                embedding.latencyMs(),
                embedding.errorMessage()
        );
    }

    private boolean imageEmbeddingConfigured() {
        RagProperties.MultimodalIngest config = ragProperties.getMultimodalIngest();
        return config.isEnabled()
                && config.isVisionEmbeddingEnabled()
                && multimodalVectorStore.enabled();
    }

    private Map<String, Object> imageAssetMetadata(Map<String, Object> ingestionMetadata,
                                                   OcrExtractionResult ocrResult,
                                                   VisionEmbeddingResult embedding,
                                                   VisualValidationResult validation) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (ingestionMetadata != null) {
            metadata.putAll(ingestionMetadata);
        }
        metadata.put("ocr_status", ocrResult.status());
        metadata.put("ocr_confidence", ocrResult.confidence());
        metadata.put("ocr_provider", ocrResult.provider());
        metadata.put("ocr_model", ocrResult.model());
        metadata.put("ocr_error_message", ocrResult.errorMessage());
        metadata.put("visual_schema_errors", validation.schemaErrors());
        metadata.put("image_embedding_status", embedding.status());
        metadata.put("image_embedding_model", embedding.model());
        metadata.put("image_embedding_dimension", embedding.dimension());
        metadata.put("image_embedding_error_message", embedding.errorMessage());
        metadata.put("image_embedding_updated_at", LocalDateTime.now().toString());
        metadata.put("review_status", reviewStatus(validation, ocrResult, embedding));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return metadata;
    }

    private String reviewStatus(VisualValidationResult validation,
                                OcrExtractionResult ocrResult,
                                VisionEmbeddingResult embedding) {
        return imageAssetReviewPolicy.initialStatus(
                validation.schemaValid(),
                validation.confidence(),
                ocrResult.confidence(),
                embedding.status(),
                ragProperties.getMultimodalIngest().getLowConfidenceThreshold()
        );
    }

    private List<Float> vectorAsList(Embedding embedding) {
        return embedding == null ? List.of() : embedding.vectorAsList();
    }

    private String contentTypeValue(ExtractedImageKnowledge image) {
        return image.contentType() == null ? "image" : image.contentType().name().toLowerCase(Locale.ROOT);
    }

    private String coordinateJson(ExtractedImageKnowledge image) {
        if (image.coordinate() == null) {
            return "{}";
        }
        return """
                {"x":%s,"y":%s,"width":%s,"height":%s,"pageWidth":%s,"pageHeight":%s}
                """.formatted(
                image.coordinate().x(),
                image.coordinate().y(),
                image.coordinate().width(),
                image.coordinate().height(),
                image.coordinate().pageWidth(),
                image.coordinate().pageHeight()
        ).trim();
    }

    private String metadataJson(ExtractedImageKnowledge image, VisualValidationResult validation) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("imageId", image.id());
        root.put("ocrText", image.ocrText());
        root.put("visualStatus", validation.status());
        root.put("visualSchemaValid", validation.schemaValid());
        root.put("visualConfidence", validation.confidence());
        try {
            JsonNode visualNode = objectMapper.readTree(validation.normalizedJson());
            root.set("visualStructuredContent", visualNode);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            root.put("visualStructuredContentRaw", validation.normalizedJson());
            return root.toString();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String value(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        Long parsed = parseLong(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Invalid long value: {}", value);
            return null;
        }
    }

    private void removeNullValues(Map<String, Object> metadata) {
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}

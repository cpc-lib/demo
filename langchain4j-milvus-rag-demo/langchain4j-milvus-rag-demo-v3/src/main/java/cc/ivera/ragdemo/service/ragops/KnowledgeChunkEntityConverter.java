package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.model.knowledge.ChunkStatus;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class KnowledgeChunkEntityConverter {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public KnowledgeChunkEntityConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RagDocumentChunk toEntity(KnowledgeChunkRecord record) {
        RagDocumentChunk entity = new RagDocumentChunk();
        entity.setTenantId(parseLongOrDefault(record.tenantId(), 0L));
        entity.setTenantExternalId(record.tenantId());
        entity.setKnowledgeBaseId(0L);
        entity.setDocumentId(parseLongOrDefault(record.documentId(), 0L));
        entity.setSourceDocumentId(record.documentId());
        entity.setDocumentVersionId(0L);
        entity.setChunkUid(record.chunkId());
        entity.setChunkVersion(record.version());
        entity.setChunkStatus(statusName(record.status()));
        entity.setCurrentFlag(record.current());
        entity.setChunkIndex(0);
        entity.setParentChunkUid(record.parentChunkId());
        entity.setSource(record.source());
        entity.setFileName(record.fileName());
        entity.setContentType(record.contentType());
        entity.setPageStart(record.pageNo());
        entity.setPageEnd(record.pageNo());
        entity.setTitle(record.sectionTitle());
        entity.setSectionPath(record.sectionTitle());
        entity.setImageUrl(record.imageUrl());
        entity.setImageCaption(record.imageCaption());
        entity.setImageNumber(record.imageNumber());
        entity.setPermissionTags(String.join(",", safeList(record.permissionTags())));
        entity.setContent(record.textContent());
        entity.setContentSummary(truncate(record.textContent(), 500));
        entity.setContentHash(RagHashing.sha256Hex(record.textContent() == null ? "" : record.textContent()));
        entity.setCharacterCount(record.textContent() == null ? 0 : record.textContent().length());
        entity.setTokenCount(estimateTokenCount(record.textContent()));
        entity.setVectorStoreType("milvus");
        entity.setVectorCollection(record.milvusCollection());
        entity.setMilvusAlias(record.milvusAlias());
        entity.setVectorId(first(record.textVectorIds()));
        entity.setTextVectorIds(writeList(record.textVectorIds()));
        entity.setImageVectorIds(writeList(record.imageVectorIds()));
        entity.setEmbeddingStatus(embeddingStatus(record));
        entity.setMetadataJson(record.metadataJson());
        entity.setCreatedAt(toLocalDateTime(record.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(record.updatedAt()));
        entity.setIsDeleted(record.status() == ChunkStatus.DELETED ? 1 : 0);
        return entity;
    }

    public KnowledgeChunkRecord toRecord(RagDocumentChunk entity) {
        return KnowledgeChunkRecord.builder()
                .chunkId(entity.getChunkUid())
                .documentId(StringUtils.hasText(entity.getSourceDocumentId())
                        ? entity.getSourceDocumentId()
                        : stringValue(entity.getDocumentId()))
                .source(entity.getSource())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .textContent(entity.getContent())
                .textVectorIds(readList(entity.getTextVectorIds(), entity.getVectorId()))
                .imageVectorIds(readList(entity.getImageVectorIds(), null))
                .imageUrl(entity.getImageUrl())
                .pageNo(entity.getPageStart())
                .sectionTitle(entity.getTitle())
                .imageCaption(entity.getImageCaption())
                .imageNumber(entity.getImageNumber())
                .parentChunkId(entity.getParentChunkUid())
                .permissionTags(splitTags(entity.getPermissionTags()))
                .tenantId(StringUtils.hasText(entity.getTenantExternalId())
                        ? entity.getTenantExternalId()
                        : stringValue(entity.getTenantId()))
                .version(entity.getChunkVersion() == null ? 1 : entity.getChunkVersion())
                .status(parseStatus(entity.getChunkStatus()))
                .current(Boolean.TRUE.equals(entity.getCurrentFlag()))
                .milvusAlias(entity.getMilvusAlias())
                .milvusCollection(entity.getVectorCollection())
                .metadataJson(entity.getMetadataJson())
                .createdAt(toInstant(entity.getCreatedAt()))
                .updatedAt(toInstant(entity.getUpdatedAt()))
                .build();
    }

    private String statusName(ChunkStatus status) {
        return (status == null ? ChunkStatus.ACTIVE : status).name();
    }

    private ChunkStatus parseStatus(String status) {
        return StringUtils.hasText(status) ? ChunkStatus.valueOf(status) : ChunkStatus.ACTIVE;
    }

    private int embeddingStatus(KnowledgeChunkRecord record) {
        if (record.status() != ChunkStatus.ACTIVE) {
            return 0;
        }
        return safeList(record.textVectorIds()).isEmpty() ? 1 : 2;
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(safeList(values));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize chunk vector ids", e);
        }
    }

    private List<String> readList(String json, String fallback) {
        if (StringUtils.hasText(json)) {
            try {
                return objectMapper.readValue(json, STRING_LIST);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize chunk vector ids", e);
            }
        }
        return StringUtils.hasText(fallback) ? List.of(fallback) : List.of();
    }

    private List<String> splitTags(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).toList();
    }

    private String first(List<String> values) {
        List<String> safeValues = safeList(values);
        return safeValues.isEmpty() ? null : safeValues.get(0);
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private int estimateTokenCount(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}

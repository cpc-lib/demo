package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.model.knowledge.ChunkStatus;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.ragops.KnowledgeChunkEntityConverter;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ChunkVersionService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;

    private final RagDocumentChunkMapper chunkMapper;
    private final KnowledgeChunkEntityConverter chunkEntityConverter;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public Optional<Integer> latestVersion(String chunkId) {
        RagDocumentChunk latest = chunkMapper.selectOne(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunkId)
                .orderByDesc(RagDocumentChunk::getChunkVersion)
                .last("LIMIT 1"));
        return Optional.ofNullable(latest).map(RagDocumentChunk::getChunkVersion);
    }

    public Optional<KnowledgeChunkRecord> activeRecord(String chunkId) {
        RagDocumentChunk entity = chunkMapper.selectOne(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunkId)
                .eq(RagDocumentChunk::getCurrentFlag, true)
                .last("LIMIT 1"));
        return Optional.ofNullable(entity).map(chunkEntityConverter::toRecord);
    }

    public Optional<KnowledgeChunkRecord> version(String chunkId, int version) {
        return Optional.ofNullable(entity(chunkId, version)).map(chunkEntityConverter::toRecord);
    }

    public List<KnowledgeChunkRecord> versions(String chunkId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<RagDocumentChunk>()
                        .eq(RagDocumentChunk::getChunkUid, chunkId)
                        .orderByDesc(RagDocumentChunk::getChunkVersion))
                .stream()
                .map(chunkEntityConverter::toRecord)
                .toList();
    }

    public PageResponse<KnowledgeChunkRecord> pageVersions(String chunkId, PageQuery pageQuery) {
        PageQuery query = normalizePageQuery(pageQuery, "chunkVersion", "DESC");
        LambdaQueryWrapper<RagDocumentChunk> countQuery = new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunkId);
        long total = chunkMapper.selectCount(countQuery);
        LambdaQueryWrapper<RagDocumentChunk> rowsQuery = new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunkId);
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, chunkMapper.selectList(rowsQuery).stream()
                .map(chunkEntityConverter::toRecord)
                .toList());
    }

    public List<KnowledgeChunkRecord> list(String documentId,
                                           String contentType,
                                           ChunkStatus status,
                                           boolean includeHistory) {
        LambdaQueryWrapper<RagDocumentChunk> query = new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(!includeHistory, RagDocumentChunk::getCurrentFlag, true)
                .eq(StringUtils.hasText(documentId), RagDocumentChunk::getSourceDocumentId, documentId)
                .eq(StringUtils.hasText(contentType), RagDocumentChunk::getContentType, contentType)
                .eq(status != null, RagDocumentChunk::getChunkStatus, status == null ? null : status.name())
                .orderByDesc(RagDocumentChunk::getUpdatedAt);
        if (!StringUtils.hasText(documentId)) {
            ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
            query.eq(RagDocumentChunk::getMilvusAlias, activeMilvus.alias())
                    .eq(RagDocumentChunk::getVectorCollection, activeMilvus.config().getCollection());
        }
        return chunkMapper.selectList(query).stream()
                .map(chunkEntityConverter::toRecord)
                .toList();
    }

    public PageResponse<KnowledgeChunkRecord> pageList(String documentId,
                                                       String contentType,
                                                       ChunkStatus status,
                                                       boolean includeHistory,
                                                       PageQuery pageQuery) {
        PageQuery query = normalizePageQuery(pageQuery, "updatedAt", "DESC");
        long total = chunkMapper.selectCount(listQuery(documentId, contentType, status, includeHistory));
        LambdaQueryWrapper<RagDocumentChunk> rowsQuery = listQuery(documentId, contentType, status, includeHistory);
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, chunkMapper.selectList(rowsQuery).stream()
                .map(chunkEntityConverter::toRecord)
                .toList());
    }

    public List<KnowledgeChunkRecord> allRecordsForRedisRebuild() {
        return chunkMapper.selectAllForRedisRebuild()
                .stream()
                .map(chunkEntityConverter::toRecord)
                .toList();
    }

    public RagDocumentChunk insertOrUpdate(KnowledgeChunkRecord record) {
        RagDocumentChunk entity = prepareEntity(record);
        RagDocumentChunk existing = entity(record.chunkId(), record.version());
        if (existing != null) {
            entity.setId(existing.getId());
            chunkMapper.updateById(entity);
            return entity;
        }
        chunkMapper.insert(entity);
        return entity;
    }

    public void update(KnowledgeChunkRecord record) {
        RagDocumentChunk existing = entity(record.chunkId(), record.version());
        if (existing == null) {
            insertOrUpdate(record);
            return;
        }
        update(existing.getId(), record);
    }

    public void update(Long id, KnowledgeChunkRecord record) {
        RagDocumentChunk entity = prepareEntity(record);
        entity.setId(id);
        chunkMapper.updateById(entity);
    }

    public void markEmbeddingFailed(RagDocumentChunk row, String message) {
        row.setCurrentFlag(false);
        row.setChunkStatus(ChunkStatus.SUPERSEDED.name());
        row.setEmbeddingStatus(3);
        row.setMetadataJson(mergeFailureMetadata(row.getMetadataJson(), message));
        row.setUpdatedAt(java.time.LocalDateTime.now());
        chunkMapper.updateById(row);
    }

    private RagDocumentChunk entity(String chunkId, int version) {
        return chunkMapper.selectOne(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunkId)
                .eq(RagDocumentChunk::getChunkVersion, version)
                .last("LIMIT 1"));
    }

    private LambdaQueryWrapper<RagDocumentChunk> listQuery(String documentId,
                                                          String contentType,
                                                          ChunkStatus status,
                                                          boolean includeHistory) {
        LambdaQueryWrapper<RagDocumentChunk> query = new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(!includeHistory, RagDocumentChunk::getCurrentFlag, true)
                .eq(StringUtils.hasText(documentId), RagDocumentChunk::getSourceDocumentId, documentId)
                .eq(StringUtils.hasText(contentType), RagDocumentChunk::getContentType, contentType)
                .eq(status != null, RagDocumentChunk::getChunkStatus, status == null ? null : status.name());
        if (!StringUtils.hasText(documentId)) {
            ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
            query.eq(RagDocumentChunk::getMilvusAlias, activeMilvus.alias())
                    .eq(RagDocumentChunk::getVectorCollection, activeMilvus.config().getCollection());
        }
        return query;
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery, String defaultSortBy, String defaultSortDirection) {
        PageQuery query = pageQuery == null
                ? PageQuery.of(1, null, DEFAULT_PAGE_SIZE, defaultSortBy, defaultSortDirection, MAX_PAGE_SIZE)
                : pageQuery;
        return query.withDefaultSort(defaultSortBy, defaultSortDirection);
    }

    private void applyOrder(LambdaQueryWrapper<RagDocumentChunk> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagDocumentChunk::getId);
            case "chunkId", "chunkUid" -> wrapper.orderBy(true, asc, RagDocumentChunk::getChunkUid);
            case "version", "chunkVersion" -> wrapper.orderBy(true, asc, RagDocumentChunk::getChunkVersion);
            case "status", "chunkStatus" -> wrapper.orderBy(true, asc, RagDocumentChunk::getChunkStatus);
            case "contentType" -> wrapper.orderBy(true, asc, RagDocumentChunk::getContentType);
            case "pageNo", "pageStart" -> wrapper.orderBy(true, asc, RagDocumentChunk::getPageStart);
            case "createdAt" -> wrapper.orderBy(true, asc, RagDocumentChunk::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagDocumentChunk::getUpdatedAt);
            default -> wrapper.orderByDesc(RagDocumentChunk::getUpdatedAt);
        }
    }

    private RagDocumentChunk prepareEntity(KnowledgeChunkRecord record) {
        RagDocumentChunk entity = chunkEntityConverter.toEntity(record);
        if (!StringUtils.hasText(entity.getMilvusAlias()) || !StringUtils.hasText(entity.getVectorCollection())) {
            ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
            if (!StringUtils.hasText(entity.getMilvusAlias())) {
                entity.setMilvusAlias(activeMilvus.alias());
            }
            if (!StringUtils.hasText(entity.getVectorCollection())) {
                entity.setVectorCollection(activeMilvus.config().getCollection());
            }
        }
        entity.setEmbeddingModel(properties.getEmbedding().getModel());
        entity.setEmbeddingDimension(properties.getEmbedding().getDimension());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.LocalDateTime.now());
        }
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        return entity;
    }

    private String mergeFailureMetadata(String metadataJson, String message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (StringUtils.hasText(metadataJson)) {
            try {
                metadata.putAll(objectMapper.readValue(metadataJson, Map.class));
            } catch (Exception e) {
                log.warn("Failed to parse metadata JSON, keeping original: {}", e.getMessage());
                metadata.put("previous_metadata", metadataJson);
            }
        }
        metadata.put("embedding_error", truncate(message, 500));
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return metadataJson;
        }
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}

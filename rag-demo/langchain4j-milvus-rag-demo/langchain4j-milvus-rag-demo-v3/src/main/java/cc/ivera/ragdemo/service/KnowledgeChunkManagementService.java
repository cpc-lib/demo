package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.controller.KnowledgeChunkCreateRequest;
import cc.ivera.ragdemo.controller.KnowledgeChunkUpdateRequest;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.model.knowledge.ChunkRedisRebuildResponse;
import cc.ivera.ragdemo.model.knowledge.ChunkStatus;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.query.KeywordSearchIndex;
import cc.ivera.ragdemo.service.ragops.RedisPatternKeyDeletionService;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.TenantScopedRedisKeyFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KnowledgeChunkManagementService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeChunkManagementService.class);

    private static final String CHUNK_REGISTRY_PATTERN = "rag:*:knowledge:chunk:*";
    private static final String VERSION_SET_PATTERN = "rag:*:knowledge:chunk:versions:*";
    private static final String ACTIVE_VERSION_PATTERN = "rag:*:knowledge:chunk:active:*";
    private static final String DOCUMENT_INDEX_PATTERN = "rag:*:knowledge:document:*:chunks";
    private static final String STORE_INDEX_PATTERN = "rag:*:knowledge:store:*:chunks";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final EmbeddingModel embeddingModel;
    private final ChunkVersionService chunkVersionService;
    private final RedisPatternKeyDeletionService redisPatternKeyDeletionService;
    private TenantScopedRedisKeyFactory redisKeyFactory = new TenantScopedRedisKeyFactory();
    private KeywordSearchIndex keywordSearchIndex;
    private MultimodalVectorStore multimodalVectorStore;

    @Autowired(required = false)
    public void setRedisKeyFactory(TenantScopedRedisKeyFactory redisKeyFactory) {
        if (redisKeyFactory != null) {
            this.redisKeyFactory = redisKeyFactory;
        }
    }

    @Autowired(required = false)
    public void setKeywordSearchIndex(KeywordSearchIndex keywordSearchIndex) {
        this.keywordSearchIndex = keywordSearchIndex;
    }

    @Autowired(required = false)
    public void setMultimodalVectorStore(MultimodalVectorStore multimodalVectorStore) {
        this.multimodalVectorStore = multimodalVectorStore;
    }

    public int nextVersion(String chunkId) {
        return chunkVersionService.latestVersion(chunkId)
                .or(() -> latestRedisVersion(chunkId))
                .map(version -> version + 1)
                .orElse(1);
    }

    public KnowledgeChunkRecord createChunk(KnowledgeChunkCreateRequest request) {
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        String documentId = StringUtils.hasText(request.documentId()) ? request.documentId().trim() : "manual-" + UUID.randomUUID();
        String chunkId = documentId + "-manual-" + UUID.randomUUID().toString().replace("-", "");
        KnowledgeChunkRecord record = KnowledgeChunkRecord.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .source("manual")
                .contentType(defaultContentType(request.contentType()))
                .textContent(request.textContent())
                .imageUrl(request.imageUrl())
                .pageNo(request.pageNo())
                .sectionTitle(request.sectionTitle())
                .imageCaption(request.imageCaption())
                .imageNumber(request.imageNumber())
                .parentChunkId(request.parentChunkId())
                .permissionTags(safeList(request.permissionTags()))
                .tenantId(effectiveTenantId(request.tenantId()))
                .version(1)
                .status(ChunkStatus.ACTIVE)
                .current(true)
                .milvusAlias(activeMilvus.alias())
                .milvusCollection(activeMilvus.config().getCollection())
                .metadataJson(request.metadataJson())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return insertActiveVersion(record);
    }

    public KnowledgeChunkRecord updateChunk(String chunkId, KnowledgeChunkUpdateRequest request) {
        KnowledgeChunkRecord current = activeRecord(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Active chunk not found: " + chunkId));
        KnowledgeChunkRecord merged = current.toBuilder()
                .contentType(valueOrDefault(request.contentType(), current.contentType()))
                .textContent(valueOrDefault(request.textContent(), current.textContent()))
                .imageUrl(valueOrDefault(request.imageUrl(), current.imageUrl()))
                .pageNo(request.pageNo() == null ? current.pageNo() : request.pageNo())
                .sectionTitle(valueOrDefault(request.sectionTitle(), current.sectionTitle()))
                .imageCaption(valueOrDefault(request.imageCaption(), current.imageCaption()))
                .imageNumber(valueOrDefault(request.imageNumber(), current.imageNumber()))
                .parentChunkId(valueOrDefault(request.parentChunkId(), current.parentChunkId()))
                .permissionTags(request.permissionTags() == null ? current.permissionTags() : safeList(request.permissionTags()))
                .tenantId(effectiveTenantId(current.tenantId()))
                .metadataJson(valueOrDefault(request.metadataJson(), current.metadataJson()))
                .version(nextVersion(chunkId))
                .status(ChunkStatus.ACTIVE)
                .current(true)
                .textVectorIds(List.of())
                .imageVectorIds(current.imageVectorIds() == null ? List.of() : current.imageVectorIds())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return insertActiveVersion(merged);
    }

    public KnowledgeChunkRecord rollback(String chunkId, int targetVersion) {
        KnowledgeChunkRecord target = getVersion(chunkId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException("Chunk version not found: " + chunkId + " v" + targetVersion));
        KnowledgeChunkRecord current = activeRecord(chunkId).orElse(null);
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        KnowledgeChunkRecord rollback = target.toBuilder()
                .version(nextVersion(chunkId))
                .status(ChunkStatus.ACTIVE)
                .current(true)
                .textVectorIds(List.of())
                .milvusAlias(current == null ? activeMilvus.alias() : current.milvusAlias())
                .milvusCollection(current == null ? activeMilvus.config().getCollection() : current.milvusCollection())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return insertActiveVersion(rollback);
    }

    public KnowledgeChunkRecord deleteChunk(String chunkId) {
        KnowledgeChunkRecord current = activeRecord(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Current chunk not found: " + chunkId));
        return createStatusVersion(current, ChunkStatus.DELETED);
    }

    public KnowledgeChunkRecord disableChunk(String chunkId) {
        KnowledgeChunkRecord current = activeRecord(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Current chunk not found: " + chunkId));
        return createStatusVersion(current, ChunkStatus.DISABLED);
    }

    private KnowledgeChunkRecord createStatusVersion(KnowledgeChunkRecord current, ChunkStatus status) {
        KnowledgeChunkRecord statusRecord = current.toBuilder()
                .version(nextVersion(current.chunkId()))
                .status(status)
                .current(true)
                .textVectorIds(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        chunkVersionService.insertOrUpdate(statusRecord);
        removeVectors(current);
        KnowledgeChunkRecord superseded = current.toBuilder()
                .status(ChunkStatus.SUPERSEDED)
                .current(false)
                .textVectorIds(List.of())
                .updatedAt(Instant.now())
                .build();
        chunkVersionService.update(superseded);
        saveRecord(superseded);
        saveRecord(statusRecord);
        redisTemplate.opsForValue().set(activeVersionKey(statusRecord), String.valueOf(statusRecord.version()));
        indexRecord(statusRecord);
        syncKeywordIndex(statusRecord);
        return statusRecord;
    }

    public KnowledgeChunkRecord registerEmbeddedActiveChunk(KnowledgeChunkRecord record) {
        return replaceActive(record, true);
    }

    public Optional<KnowledgeChunkRecord> activeRecord(String chunkId) {
        Optional<KnowledgeChunkRecord> mysqlRecord = chunkVersionService.activeRecord(chunkId);
        if (mysqlRecord.isPresent()) {
            return mysqlRecord;
        }
        String activeVersion = redisTemplate.opsForValue().get(activeVersionKey(chunkId));
        if (!StringUtils.hasText(activeVersion)) {
            return Optional.empty();
        }
        return getVersion(chunkId, Integer.parseInt(activeVersion));
    }

    public Optional<KnowledgeChunkRecord> getVersion(String chunkId, int version) {
        Optional<KnowledgeChunkRecord> mysqlRecord = chunkVersionService.version(chunkId, version);
        if (mysqlRecord.isPresent()) {
            return mysqlRecord;
        }
        String json = redisTemplate.opsForValue().get(recordKey(chunkId, version));
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, KnowledgeChunkRecord.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read chunk record: " + chunkId + " v" + version, e);
        }
    }

    public List<KnowledgeChunkRecord> versions(String chunkId) {
        List<KnowledgeChunkRecord> mysqlRecords = chunkVersionService.versions(chunkId);
        if (!mysqlRecords.isEmpty()) {
            return mysqlRecords;
        }
        return redisVersionNumbers(chunkId).stream()
                .map(version -> getVersion(chunkId, version))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(KnowledgeChunkRecord::version).reversed())
                .toList();
    }

    public PageResponse<KnowledgeChunkRecord> pageVersions(String chunkId, PageQuery pageQuery) {
        PageResponse<KnowledgeChunkRecord> mysqlPage = chunkVersionService.pageVersions(chunkId, pageQuery);
        if (mysqlPage.total() > 0) {
            return mysqlPage;
        }
        return PageResponse.slice(versions(chunkId), pageQuery);
    }

    public List<KnowledgeChunkRecord> list(String documentId, String contentType, ChunkStatus status, boolean includeHistory) {
        List<KnowledgeChunkRecord> mysqlRecords = chunkVersionService.list(documentId, contentType, status, includeHistory);
        if (!mysqlRecords.isEmpty()) {
            return mysqlRecords;
        }
        List<String> chunkIds = chunkIdsForList(documentId);
        List<KnowledgeChunkRecord> records = new ArrayList<>();
        for (String chunkId : chunkIds) {
            if (includeHistory) {
                records.addAll(versions(chunkId));
            } else {
                activeRecord(chunkId).ifPresent(records::add);
            }
        }
        return records.stream()
                .filter(record -> !StringUtils.hasText(contentType) || contentType.equalsIgnoreCase(record.contentType()))
                .filter(record -> status == null || status == record.status())
                .sorted(Comparator.comparing(KnowledgeChunkRecord::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public PageResponse<KnowledgeChunkRecord> pageList(String documentId,
                                                       String contentType,
                                                       ChunkStatus status,
                                                       boolean includeHistory,
                                                       PageQuery pageQuery) {
        PageResponse<KnowledgeChunkRecord> mysqlPage = chunkVersionService.pageList(
                documentId,
                contentType,
                status,
                includeHistory,
                pageQuery
        );
        if (mysqlPage.total() > 0) {
            return mysqlPage;
        }
        return PageResponse.slice(list(documentId, contentType, status, includeHistory), pageQuery);
    }

    public ChunkRedisRebuildResponse rebuildRedisRegistryFromMysql(boolean clearExisting) {
        List<KnowledgeChunkRecord> records = chunkVersionService.allRecordsForRedisRebuild();
        int deletedKeys = clearExisting ? clearRedisChunkRegistry() : 0;

        int activeCount = 0;
        for (KnowledgeChunkRecord record : records) {
            saveRecord(record);
            indexRecord(record);
            if (record.current()) {
                activeCount++;
                redisTemplate.opsForValue().set(activeVersionKey(record), String.valueOf(record.version()));
            }
        }
        int chunkCount = records.stream()
                .map(KnowledgeChunkRecord::chunkId)
                .collect(Collectors.toSet())
                .size();
        return new ChunkRedisRebuildResponse(records.size(), chunkCount, activeCount, deletedKeys);
    }

    public KnowledgeChunkRecord insertActiveVersion(KnowledgeChunkRecord record) {
        Optional<KnowledgeChunkRecord> previous = activeRecord(record.chunkId());
        KnowledgeChunkRecord pending = record.toBuilder()
                .textVectorIds(List.of())
                .status(ChunkStatus.ACTIVE)
                .current(true)
                .updatedAt(Instant.now())
                .build();
        RagDocumentChunk row = chunkVersionService.insertOrUpdate(pending);
        TextSegment segment = TextSegment.from(record.textContent(), Metadata.from(metadataFor(record)));
        String vectorId;
        try {
            Response<Embedding> response = embeddingModel.embed(segment);
            vectorId = milvusContextFor(record).store().add(response.content(), segment);
        } catch (Exception e) {
            chunkVersionService.markEmbeddingFailed(row, e.getMessage());
            throw new IllegalStateException("Failed to embed chunk: " + record.chunkId(), e);
        }
        KnowledgeChunkRecord embedded = record.toBuilder()
                .textVectorIds(List.of(vectorId))
                .status(ChunkStatus.ACTIVE)
                .current(true)
                .updatedAt(Instant.now())
                .build();
        chunkVersionService.update(row.getId(), embedded);
        return replaceActive(embedded, previous, false);
    }

    public Map<String, Object> metadataFor(KnowledgeChunkRecord record) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", StringUtils.hasText(record.source()) ? record.source() : "chunk-management");
        metadata.put("fileName", record.fileName());
        metadata.put("document_id", record.documentId());
        metadata.put("content_type", record.contentType());
        metadata.put("chunk_id", record.chunkId());
        metadata.put("version", record.version());
        metadata.put("chunk_status", record.status() == null ? ChunkStatus.ACTIVE.name() : record.status().name());
        metadata.put("current", String.valueOf(record.current()));
        metadata.put("image_url", record.imageUrl());
        metadata.put("page_no", record.pageNo());
        metadata.put("section_title", record.sectionTitle());
        metadata.put("image_caption", record.imageCaption());
        metadata.put("image_number", record.imageNumber());
        metadata.put("parent_chunk_id", record.parentChunkId());
        metadata.put("permission_tags", String.join(",", safeList(record.permissionTags())));
        metadata.put("tenant_id", record.tenantId());
        metadata.put("milvusAlias", record.milvusAlias());
        metadata.put("milvusCollection", record.milvusCollection());
        metadata.put("metadata_json", record.metadataJson());
        metadata.put("chunkId", record.chunkId());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return metadata;
    }

    private KnowledgeChunkRecord replaceActive(KnowledgeChunkRecord newRecord, boolean insertMysql) {
        Optional<KnowledgeChunkRecord> previous = activeRecord(newRecord.chunkId());
        if (insertMysql) {
            chunkVersionService.insertOrUpdate(newRecord);
        }
        return replaceActive(newRecord, previous, false);
    }

    private KnowledgeChunkRecord replaceActive(KnowledgeChunkRecord newRecord,
                                               Optional<KnowledgeChunkRecord> previous,
                                               boolean insertMysql) {
        if (insertMysql) {
            chunkVersionService.insertOrUpdate(newRecord);
        }
        previous.ifPresent(current -> {
            if (current.version() != newRecord.version()) {
                removeVectors(current);
                KnowledgeChunkRecord superseded = current.toBuilder()
                        .status(ChunkStatus.SUPERSEDED)
                        .current(false)
                        .updatedAt(Instant.now())
                        .build();
                chunkVersionService.update(superseded);
                saveRecord(superseded);
                syncKeywordIndex(superseded);
            }
        });
        saveRecord(newRecord);
        redisTemplate.opsForValue().set(activeVersionKey(newRecord), String.valueOf(newRecord.version()));
        indexRecord(newRecord);
        syncKeywordIndex(newRecord);
        return newRecord;
    }

    private void syncKeywordIndex(KnowledgeChunkRecord record) {
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            return;
        }
        try {
            if (record.current() && record.status() == ChunkStatus.ACTIVE) {
                keywordSearchIndex.upsert(record);
            } else {
                keywordSearchIndex.delete(record.chunkId());
            }
        } catch (Exception e) {
            log.warn("Failed to sync keyword index for chunk {}: {}", record.chunkId(), e.getMessage());
        }
    }

    private void saveRecord(KnowledgeChunkRecord record) {
        try {
            redisTemplate.opsForValue().set(recordKey(record), objectMapper.writeValueAsString(record));
            redisTemplate.opsForSet().add(versionSetKey(record), String.valueOf(record.version()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to save chunk record: " + record.chunkId(), e);
        }
    }

    private void indexRecord(KnowledgeChunkRecord record) {
        if (StringUtils.hasText(record.milvusAlias()) && StringUtils.hasText(record.milvusCollection())) {
            redisTemplate.opsForSet().add(storeIndexKey(record), record.chunkId());
        }
        if (StringUtils.hasText(record.documentId())) {
            redisTemplate.opsForSet().add(documentIndexKey(record), record.chunkId());
        }
    }

    private int clearRedisChunkRegistry() {
        int deleted = 0;
        deleted += deleteKeys(CHUNK_REGISTRY_PATTERN);
        deleted += deleteKeys(VERSION_SET_PATTERN);
        deleted += deleteKeys(ACTIVE_VERSION_PATTERN);
        deleted += deleteKeys(DOCUMENT_INDEX_PATTERN);
        deleted += deleteKeys(STORE_INDEX_PATTERN);
        return deleted;
    }

    private int deleteKeys(String pattern) {
        return redisPatternKeyDeletionService.deleteByPattern(pattern);
    }

    private void removeVectors(KnowledgeChunkRecord record) {
        List<String> vectorIds = safeList(record.textVectorIds());
        if (!vectorIds.isEmpty()) {
            try {
                EmbeddingStore<TextSegment> store = milvusContextFor(record).store();
                store.removeAll(vectorIds);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to remove vectors for chunk: " + record.chunkId(), e);
            }
        }
        List<String> imageVectorIds = safeList(record.imageVectorIds());
        if (multimodalVectorStore != null && (!imageVectorIds.isEmpty() || StringUtils.hasText(record.chunkId()))) {
            try {
                if (!imageVectorIds.isEmpty()) {
                    multimodalVectorStore.deleteByIds(imageVectorIds);
                } else {
                    multimodalVectorStore.deleteByChunkUid(record.chunkId());
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to remove image vectors for chunk: " + record.chunkId(), e);
            }
        }
    }

    private Optional<Integer> latestRedisVersion(String chunkId) {
        return redisVersionNumbers(chunkId).stream().max(Integer::compareTo);
    }

    private List<Integer> redisVersionNumbers(String chunkId) {
        var values = redisTemplate.opsForSet().members(versionSetKey(chunkId));
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (Exception ignore) {
                        return null;
                    }
                })
                .filter(value -> value != null)
                .sorted()
                .toList();
    }

    private List<String> chunkIdsForList(String documentId) {
        String key;
        if (StringUtils.hasText(documentId)) {
            key = documentIndexKey(documentId.trim());
        } else {
            ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
            key = storeIndexKey(activeMilvus.alias(), activeMilvus.config().getCollection());
        }
        var values = redisTemplate.opsForSet().members(key);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().sorted().toList();
    }

    private String recordKey(String chunkId, int version) {
        return redisKeyFactory.chunkRecordKey(currentTenantKey(), chunkId, version);
    }

    private String recordKey(KnowledgeChunkRecord record) {
        return redisKeyFactory.chunkRecordKey(recordTenantKey(record), record.chunkId(), record.version());
    }

    private String versionSetKey(String chunkId) {
        return redisKeyFactory.chunkVersionsKey(currentTenantKey(), chunkId);
    }

    private String versionSetKey(KnowledgeChunkRecord record) {
        return redisKeyFactory.chunkVersionsKey(recordTenantKey(record), record.chunkId());
    }

    private String activeVersionKey(String chunkId) {
        return redisKeyFactory.chunkActiveVersionKey(currentTenantKey(), chunkId);
    }

    private String activeVersionKey(KnowledgeChunkRecord record) {
        return redisKeyFactory.chunkActiveVersionKey(recordTenantKey(record), record.chunkId());
    }

    private String documentIndexKey(String documentId) {
        return redisKeyFactory.documentChunksKey(currentTenantKey(), documentId);
    }

    private String documentIndexKey(KnowledgeChunkRecord record) {
        return redisKeyFactory.documentChunksKey(recordTenantKey(record), record.documentId());
    }

    private String storeIndexKey(String alias, String collection) {
        return redisKeyFactory.storeChunksKey(currentTenantKey(), alias, collection);
    }

    private String storeIndexKey(KnowledgeChunkRecord record) {
        return redisKeyFactory.storeChunksKey(recordTenantKey(record), record.milvusAlias(), record.milvusCollection());
    }

    private String currentTenantKey() {
        return TenantContextHolder.currentTenantId()
                .map(String::valueOf)
                .orElse("0");
    }

    private String recordTenantKey(KnowledgeChunkRecord record) {
        if (record != null && StringUtils.hasText(record.tenantId())) {
            return record.tenantId().trim();
        }
        return currentTenantKey();
    }

    private String effectiveTenantId(String requestedTenantId) {
        return TenantContextHolder.currentTenantId()
                .map(String::valueOf)
                .orElseGet(() -> StringUtils.hasText(requestedTenantId) ? requestedTenantId.trim() : "0");
    }

    private ActiveMilvusContext milvusContextFor(KnowledgeChunkRecord record) {
        if (record == null) {
            return dynamicMilvusStoreManager.current();
        }
        return dynamicMilvusStoreManager.context(record.milvusAlias(), record.milvusCollection());
    }

    private String defaultContentType(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "text";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).toList();
    }
}

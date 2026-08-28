package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.MultimodalCollectionStatus;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorRecord;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchHit;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchRequest;
import cc.ivera.ragdemo.service.ragops.ImageAssetReviewPolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NativeMilvusMultimodalStore implements MultimodalVectorStore {

    private static final String ID = "id";
    private static final String CHUNK_UID = "chunk_uid";
    private static final String IMAGE_ID = "image_id";
    private static final String DOCUMENT_ID = "document_id";
    private static final String DOCUMENT_VERSION_ID = "document_version_id";
    private static final String KNOWLEDGE_BASE_ID = "knowledge_base_id";
    private static final String TENANT_ID = "tenant_id";
    private static final String CONTENT_TYPE = "content_type";
    private static final String MODALITY = "modality";
    private static final String REVIEW_STATUS = "review_status";
    private static final String EMBEDDING_MODEL = "embedding_model";
    private static final String EMBEDDING_DIMENSION = "embedding_dimension";
    private static final String PAGE_NO = "page_no";
    private static final String SECTION_TITLE = "section_title";
    private static final String PERMISSION_TAGS = "permission_tags";
    private static final String IS_CURRENT = "is_current";
    private static final String CREATED_AT = "created_at";

    private final RagProperties properties;
    private final TenantMilvusCollectionResolver collectionResolver;

    @Autowired
    public NativeMilvusMultimodalStore(RagProperties properties,
                                       TenantMilvusCollectionResolver collectionResolver) {
        this.properties = properties;
        this.collectionResolver = collectionResolver;
    }

    @Override
    public boolean enabled() {
        return properties.getMilvus().isMultimodalEnabled();
    }

    @Override
    public MultimodalCollectionStatus ensureCollection() {
        return ensureCollection(TenantContextHolder.currentTenantId().orElse(null));
    }

    private MultimodalCollectionStatus ensureCollection(Long tenantId) {
        RagProperties.Milvus milvus = properties.getMilvus();
        String collectionName = collectionName(tenantId);
        if (!enabled()) {
            return new MultimodalCollectionStatus(
                    false,
                    collectionName,
                    milvus.getTextVectorField(),
                    milvus.getImageVectorField(),
                    properties.getEmbedding().getDimension(),
                    properties.getMultimodalIngest().getVisionEmbeddingDimension(),
                    fieldNames(),
                    "DISABLED",
                    "Native multimodal Milvus collection is disabled"
            );
        }
        try (ClientHandle handle = connect()) {
            MilvusServiceClient client = handle.client();
            R<Boolean> exists = client.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            assertSuccess(exists, "check multimodal collection");
            boolean collectionExists = Boolean.TRUE.equals(exists.getData());
            if (!collectionExists) {
                if (!milvus.isAutoCreateMultimodalCollection()) {
                    return new MultimodalCollectionStatus(
                            true,
                            collectionName,
                            milvus.getTextVectorField(),
                            milvus.getImageVectorField(),
                            properties.getEmbedding().getDimension(),
                            properties.getMultimodalIngest().getVisionEmbeddingDimension(),
                            fieldNames(),
                            "MISSING",
                            "Multimodal collection does not exist and auto creation is disabled"
                    );
                }
                createCollection(client, collectionName);
                createIndex(client, collectionName, milvus.getTextVectorField());
                createIndex(client, collectionName, milvus.getImageVectorField());
            }
            R<RpcStatus> loaded = client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSyncLoad(false)
                    .build());
            assertSuccess(loaded, "load multimodal collection");
            return new MultimodalCollectionStatus(
                    true,
                    collectionName,
                    milvus.getTextVectorField(),
                    milvus.getImageVectorField(),
                    properties.getEmbedding().getDimension(),
                    properties.getMultimodalIngest().getVisionEmbeddingDimension(),
                    fieldNames(),
                    collectionExists ? "READY" : "CREATED",
                    collectionExists ? "Multimodal collection is ready" : "Multimodal collection was created and loaded"
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to ensure multimodal collection", ex);
        }
    }

    @Override
    public List<String> fieldNames() {
        List<String> fields = new ArrayList<>(List.of(
                ID,
                CHUNK_UID,
                IMAGE_ID,
                DOCUMENT_ID,
                DOCUMENT_VERSION_ID,
                KNOWLEDGE_BASE_ID,
                TENANT_ID,
                CONTENT_TYPE,
                MODALITY,
                REVIEW_STATUS,
                properties.getMilvus().getTextVectorField(),
                properties.getMilvus().getImageVectorField(),
                EMBEDDING_MODEL,
                EMBEDDING_DIMENSION,
                PAGE_NO,
                SECTION_TITLE,
                PERMISSION_TAGS,
                IS_CURRENT,
                CREATED_AT
        ));
        return List.copyOf(fields);
    }

    @Override
    public String upsert(MultimodalVectorRecord record) {
        if (!enabled()) {
            return null;
        }
        validateRecord(record);
        String collectionName = collectionName(record.tenantId());
        ensureCollection(record.tenantId());
        try (ClientHandle handle = connect()) {
            MilvusServiceClient client = handle.client();
            deleteByIds(client, collectionName, List.of(record.id()));
            JsonObject row = toRow(record);
            R<MutationResult> inserted = client.insert(InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withRows(List.of(row))
                    .build());
            assertSuccess(inserted, "insert multimodal vector record");
            return record.id();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upsert multimodal vector record: " + record.id(), ex);
        }
    }

    @Override
    public List<MultimodalVectorSearchHit> search(MultimodalVectorSearchRequest request) {
        if (!enabled() || request == null || request.vector() == null || request.vector().isEmpty()) {
            return List.of();
        }
        String collectionName = collectionName(request.tenantId());
        ensureCollection(request.tenantId());
        try (ClientHandle handle = connect()) {
            MilvusServiceClient client = handle.client();
            String vectorField = vectorField(request.modality());
            R<SearchResults> searched = client.search(SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName(vectorField)
                    .withMetricType(metricType())
                    .withVectors(List.of(request.vector()))
                    .withTopK(Math.max(1, request.topK()))
                    .withExpr(searchExpr(request))
                    .withOutFields(outFields())
                    .withParams(properties.getMilvus().getMultimodalSearchParams())
                    .build());
            assertSuccess(searched, "search multimodal vectors");
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searched.getData().getResults());
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
            List<MultimodalVectorSearchHit> hits = new ArrayList<>();
            for (SearchResultsWrapper.IDScore score : scores) {
                double value = score.getScore();
                if (request.minScore() > 0 && value < request.minScore()) {
                    continue;
                }
                Map<String, Object> fields = new LinkedHashMap<>(score.getFieldValues());
                fields.put("vector_field", vectorField);
                fields.put("milvus_id", firstNonBlank(score.getStrID(), stringValue(fields.get(ID))));
                hits.add(new MultimodalVectorSearchHit(
                        firstNonBlank(score.getStrID(), stringValue(fields.get(ID))),
                        stringValue(fields.get(CHUNK_UID)),
                        stringValue(fields.get(IMAGE_ID)),
                        longValue(fields.get(DOCUMENT_ID)),
                        longValue(fields.get(DOCUMENT_VERSION_ID)),
                        longValue(fields.get(KNOWLEDGE_BASE_ID)),
                        longValue(fields.get(TENANT_ID)),
                        stringValue(fields.get(CONTENT_TYPE)),
                        stringValue(fields.get(MODALITY)),
                        value,
                        integerValue(fields.get(PAGE_NO)),
                        stringValue(fields.get(SECTION_TITLE)),
                        fields
                ));
            }
            return hits;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to search multimodal vectors", ex);
        }
    }

    @Override
    public void deleteByIds(List<String> ids) {
        deleteByIds(TenantContextHolder.currentTenantId().orElse(null), ids);
    }

    @Override
    public void deleteByIds(Long tenantId, List<String> ids) {
        if (!enabled() || ids == null || ids.stream().noneMatch(StringUtils::hasText)) {
            return;
        }
        try (ClientHandle handle = connect()) {
            deleteByIds(handle.client(), collectionName(tenantId), ids);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete multimodal vector records", ex);
        }
    }

    @Override
    public void deleteByChunkUid(String chunkUid) {
        deleteByChunkUid(TenantContextHolder.currentTenantId().orElse(null), chunkUid);
    }

    @Override
    public void deleteByChunkUid(Long tenantId, String chunkUid) {
        if (!enabled() || !StringUtils.hasText(chunkUid)) {
            return;
        }
        try (ClientHandle handle = connect()) {
            delete(handle.client(), collectionName(tenantId), CHUNK_UID + " == " + quote(chunkUid));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete multimodal vectors for chunk: " + chunkUid, ex);
        }
    }

    private void createCollection(MilvusServiceClient client, String collectionName) {
        R<RpcStatus> created = client.createCollection(CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("RAG multimodal text/image vector collection")
                .withShardsNum(2)
                .withEnableDynamicField(false)
                .withFieldTypes(fieldTypes())
                .build());
        assertSuccess(created, "create multimodal collection");
    }

    List<FieldType> fieldTypes() {
        RagProperties.Milvus milvus = properties.getMilvus();
        return List.of(
                varchar(ID, 256, true),
                varchar(CHUNK_UID, 256, false),
                varchar(IMAGE_ID, 256, false),
                scalar(DOCUMENT_ID, DataType.Int64),
                scalar(DOCUMENT_VERSION_ID, DataType.Int64),
                scalar(KNOWLEDGE_BASE_ID, DataType.Int64),
                scalar(TENANT_ID, DataType.Int64),
                varchar(CONTENT_TYPE, 64, false),
                varchar(MODALITY, 32, false),
                varchar(REVIEW_STATUS, 32, false),
                vector(milvus.getTextVectorField(), properties.getEmbedding().getDimension()),
                vector(milvus.getImageVectorField(), properties.getMultimodalIngest().getVisionEmbeddingDimension()),
                varchar(EMBEDDING_MODEL, 128, false),
                scalar(EMBEDDING_DIMENSION, DataType.Int64),
                scalar(PAGE_NO, DataType.Int64),
                varchar(SECTION_TITLE, 512, false),
                varchar(PERMISSION_TAGS, 1024, false),
                scalar(IS_CURRENT, DataType.Bool),
                scalar(CREATED_AT, DataType.Int64)
        );
    }

    private FieldType varchar(String name, int maxLength, boolean primaryKey) {
        return FieldType.newBuilder()
                .withName(name)
                .withDataType(DataType.VarChar)
                .withMaxLength(maxLength)
                .withPrimaryKey(primaryKey)
                .withAutoID(false)
                .build();
    }

    private FieldType scalar(String name, DataType dataType) {
        return FieldType.newBuilder()
                .withName(name)
                .withDataType(dataType)
                .build();
    }

    private FieldType vector(String name, int dimension) {
        return FieldType.newBuilder()
                .withName(name)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();
    }

    private void createIndex(MilvusServiceClient client, String collectionName, String fieldName) {
        R<RpcStatus> indexed = client.createIndex(CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(fieldName)
                .withMetricType(metricType())
                .withIndexType(indexType())
                .withExtraParam(properties.getMilvus().getMultimodalIndexParams())
                .withSyncMode(Boolean.TRUE)
                .build());
        assertSuccess(indexed, "create multimodal index on " + fieldName);
    }

    private JsonObject toRow(MultimodalVectorRecord record) {
        JsonObject row = new JsonObject();
        row.addProperty(ID, record.id());
        row.addProperty(CHUNK_UID, nonNull(record.chunkUid()));
        row.addProperty(IMAGE_ID, nonNull(record.imageId()));
        row.addProperty(DOCUMENT_ID, defaultLong(record.documentId()));
        row.addProperty(DOCUMENT_VERSION_ID, defaultLong(record.documentVersionId()));
        row.addProperty(KNOWLEDGE_BASE_ID, defaultLong(record.knowledgeBaseId()));
        row.addProperty(TENANT_ID, defaultLong(record.tenantId()));
        row.addProperty(CONTENT_TYPE, nonNull(record.contentType()));
        row.addProperty(MODALITY, nonNull(record.modality()));
        row.addProperty(REVIEW_STATUS, nonNull(record.reviewStatus()));
        row.add(properties.getMilvus().getTextVectorField(),
                vectorArray(record.textVector(), properties.getEmbedding().getDimension()));
        row.add(properties.getMilvus().getImageVectorField(),
                vectorArray(record.imageVector(), properties.getMultimodalIngest().getVisionEmbeddingDimension()));
        row.addProperty(EMBEDDING_MODEL, nonNull(record.embeddingModel()));
        row.addProperty(EMBEDDING_DIMENSION, record.embeddingDimension() == null ? 0 : record.embeddingDimension());
        row.addProperty(PAGE_NO, record.pageNo() == null ? 0L : record.pageNo().longValue());
        row.addProperty(SECTION_TITLE, nonNull(record.sectionTitle()));
        row.addProperty(PERMISSION_TAGS, nonNull(record.permissionTags()));
        row.addProperty(IS_CURRENT, record.current());
        row.addProperty(CREATED_AT, record.createdAt() == null ? Instant.now().toEpochMilli() : record.createdAt());
        return row;
    }

    private JsonArray vectorArray(List<Float> values, int dimension) {
        JsonArray array = new JsonArray();
        List<Float> safe = values == null ? List.of() : values;
        if (safe.size() == dimension) {
            safe.forEach(array::add);
            return array;
        }
        for (int i = 0; i < dimension; i++) {
            array.add(0.0F);
        }
        return array;
    }

    private void deleteByIds(MilvusServiceClient client, String collectionName, List<String> ids) {
        List<String> safeIds = ids == null ? List.of() : ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (safeIds.isEmpty()) {
            return;
        }
        String expr = safeIds.size() == 1
                ? ID + " == " + quote(safeIds.get(0))
                : ID + " in [" + String.join(",", safeIds.stream().map(this::quote).toList()) + "]";
        delete(client, collectionName, expr);
    }

    private void delete(MilvusServiceClient client, String collectionName, String expr) {
        R<MutationResult> deleted = client.delete(DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build());
        assertSuccess(deleted, "delete multimodal vector records");
    }

    String collectionName(Long tenantId) {
        return collectionResolver.collectionForTenant(properties.getMilvus().getMultimodalCollection(), tenantId);
    }

    private String collectionName() {
        return collectionName(TenantContextHolder.currentTenantId().orElse(null));
    }

    private String searchExpr(MultimodalVectorSearchRequest request) {
        List<String> expressions = new ArrayList<>();
        expressions.add(IS_CURRENT + " == true");
        expressions.add(TENANT_ID + " == " + (request.tenantId() == null ? 0L : request.tenantId()));
        if (request.knowledgeBaseIds() != null && !request.knowledgeBaseIds().isEmpty()) {
            String ids = String.join(",", request.knowledgeBaseIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList());
            if (StringUtils.hasText(ids)) {
                expressions.add(KNOWLEDGE_BASE_ID + " in [" + ids + "]");
            }
        }
        List<String> modalities = normalizedList(request.modality() == null ? List.of() : List.of(request.modality()));
        if (!modalities.isEmpty()) {
            expressions.add(MODALITY + " in [" + quotedList(modalities) + "]");
        }
        List<String> contentTypes = normalizedList(request.contentTypes());
        if (!contentTypes.isEmpty()) {
            expressions.add(CONTENT_TYPE + " in [" + quotedList(contentTypes) + "]");
        }
        List<String> reviewStatuses = request.includeReviewPending()
                ? List.of(ImageAssetReviewPolicy.AUTO_APPROVED, ImageAssetReviewPolicy.REVIEW_APPROVED, ImageAssetReviewPolicy.REVIEW_PENDING)
                : List.of(ImageAssetReviewPolicy.AUTO_APPROVED, ImageAssetReviewPolicy.REVIEW_APPROVED);
        expressions.add(REVIEW_STATUS + " in [" + quotedList(reviewStatuses) + "]");
        return String.join(" && ", expressions);
    }

    private List<String> outFields() {
        List<String> fields = new ArrayList<>(fieldNames());
        fields.remove(properties.getMilvus().getTextVectorField());
        fields.remove(properties.getMilvus().getImageVectorField());
        return fields;
    }

    private String vectorField(String modality) {
        String normalized = StringUtils.hasText(modality) ? modality.trim().toLowerCase(Locale.ROOT) : "image";
        if ("text".equals(normalized)) {
            return properties.getMilvus().getTextVectorField();
        }
        return properties.getMilvus().getImageVectorField();
    }

    private MetricType metricType() {
        try {
            return MetricType.valueOf(properties.getMilvus().getMultimodalMetricType().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Invalid metric type value, using default COSINE: {}", e.getMessage());
            return MetricType.COSINE;
        }
    }

    private IndexType indexType() {
        try {
            return IndexType.valueOf(properties.getMilvus().getMultimodalIndexType().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Invalid index type value, using default HNSW: {}", e.getMessage());
            return IndexType.HNSW;
        }
    }

    private ClientHandle connect() {
        RagProperties.Milvus milvus = properties.getMilvus();
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvus.getHost())
                .withPort(milvus.getPort())
                .withConnectTimeout(5, TimeUnit.SECONDS);
        if (StringUtils.hasText(milvus.getUsername()) || StringUtils.hasText(milvus.getPassword())) {
            builder.withAuthorization(nonNull(milvus.getUsername()), nonNull(milvus.getPassword()));
        }
        return new ClientHandle(new MilvusServiceClient(builder.build()));
    }

    private void validateRecord(MultimodalVectorRecord record) {
        if (record == null || !StringUtils.hasText(record.id())) {
            throw new IllegalArgumentException("multimodal vector id must not be blank");
        }
        if (!StringUtils.hasText(record.modality())) {
            throw new IllegalArgumentException("multimodal vector modality must not be blank");
        }
    }

    private void assertSuccess(R<?> response, String operation) {
        if (response == null) {
            throw new IllegalStateException("Milvus " + operation + " returned no response");
        }
        if (response.getException() != null) {
            throw new IllegalStateException("Milvus " + operation + " failed: " + response.getException().getMessage(), response.getException());
        }
        Integer status = response.getStatus();
        if (status != null && status != 0) {
            throw new IllegalStateException("Milvus " + operation + " failed: " + response.getMessage());
        }
    }

    private List<String> normalizedList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String quotedList(List<String> values) {
        return String.join(",", values.stream().map(this::quote).toList());
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer integerValue(Object value) {
        Long parsed = longValue(value);
        return parsed == null ? null : parsed.intValue();
    }

    private record ClientHandle(MilvusServiceClient client) implements AutoCloseable {
        @Override
        public void close() {
            try {
                client.close(1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

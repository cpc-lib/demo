package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.controller.MilvusCollectionQueryRequest;
import cc.ivera.ragdemo.controller.MilvusCreateCollectionRequest;
import cc.ivera.ragdemo.service.vector.TenantMilvusCollectionResolver;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MilvusCollectionQueryService {

    private static final Logger log = LoggerFactory.getLogger(MilvusCollectionQueryService.class);

    private final MilvusClientV2 milvusClient;
    private final ObjectMapper objectMapper;
    private final TenantMilvusCollectionResolver collectionResolver;

    public List<String> listCollections(String databaseName) throws InterruptedException {
        milvusClient.useDatabase(safeDatabaseName(databaseName));
        return milvusClient.listCollections().getCollectionNames().stream()
                .filter(this::tenantCanAccessCollection)
                .toList();
    }

    public Map<String, Object> describeCollection(String databaseName, String collectionName) throws InterruptedException {
        String safeCollectionName = safeCollectionName(collectionName);
        assertTenantCanAccessCollection(safeCollectionName);
        milvusClient.useDatabase(safeDatabaseName(databaseName));
        var resp = milvusClient.describeCollection(
                DescribeCollectionReq.builder()
                        .collectionName(safeCollectionName)
                        .build()
        );
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> query(MilvusCollectionQueryRequest request) throws InterruptedException {
        String collectionName = safeCollectionName(request.collectionName());
        assertTenantCanAccessCollection(collectionName);
        milvusClient.useDatabase(request.safeDatabaseName());

        if (request.loadBeforeQuery()) {
            try {
                milvusClient.loadCollection(
                        LoadCollectionReq.builder()
                                .collectionName(collectionName)
                                .build()
                );
            } catch (Exception ex) {
                log.warn("loadCollection failed for '{}': {} — proceeding with query anyway",
                        collectionName, describeException(ex), ex);
            }
        }

        QueryReq queryReq = QueryReq.builder()
                .collectionName(collectionName)
                .filter(request.safeFilter())
                .outputFields(request.outputFields() == null ? List.of() : request.outputFields())
                .partitionNames(request.partitionNames() == null ? List.of() : request.partitionNames())
                .offset(request.offset())
                .limit(request.safeLimit())
                .build();

        try {
            QueryResp queryResp = milvusClient.query(queryReq);
            Map<String, Object> raw = objectMapper.convertValue(queryResp, new TypeReference<>() {});

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("databaseName", request.safeDatabaseName());
            result.put("collectionName", collectionName);
            result.put("filter", request.safeFilter());
            result.put("offset", request.offset());
            result.put("limit", request.safeLimit());
            result.put("rows", raw.getOrDefault("queryResults", List.of()));
            result.put("raw", raw);
            return result;
        } catch (Exception ex) {
            String detail = describeException(ex);
            log.error("Milvus query failed for collection='{}', filter='{}', outputFields={}, offset={}, limit={}: {}",
                    collectionName, request.safeFilter(), request.outputFields(),
                    request.offset(), request.safeLimit(), detail, ex);
            // Re-throw with a descriptive message so the global exception handler can surface it
            throw new IllegalStateException("Milvus query failed: " + detail, ex);
        }
    }

    /**
     * Extracts a human-readable description from a Milvus exception. Milvus SDK 2.3.x
     * {@code MilvusClientException} often carries a null top-level message; the actual error is
     * accessible via {@code getErrorCode()}, {@code getServerErrCode()}, and
     * {@code getLegacyServerCode()}.
     */
    private String describeException(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getSimpleName());
        String msg = ex.getMessage();
        if (msg != null && !msg.isBlank()) {
            sb.append(": ").append(msg);
        }
        // Extract Milvus-specific error fields via reflection
        appendMilvusErrorFields(sb, ex);
        Throwable cause = ex.getCause();
        if (cause != null && cause != ex) {
            sb.append(" -> cause: ").append(cause.getClass().getSimpleName());
            String causeMsg = cause.getMessage();
            if (causeMsg != null && !causeMsg.isBlank()) {
                sb.append(": ").append(causeMsg);
            }
            appendMilvusErrorFields(sb, cause);
        }
        return sb.toString();
    }

    private void appendMilvusErrorFields(StringBuilder sb, Throwable ex) {
        Class<?> cls = ex.getClass();
        try {
            var ecMethod = cls.getMethod("getErrorCode");
            Object errorCode = ecMethod.invoke(ex);
            if (errorCode != null) {
                sb.append(" [errorCode=").append(errorCode).append("]");
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed to extract getErrorCode from {}", cls.getName(), e);
        }
        try {
            var secMethod = cls.getMethod("getServerErrCode");
            int serverErrCode = (int) secMethod.invoke(ex);
            if (serverErrCode != 0) {
                sb.append(" [serverErrCode=").append(serverErrCode).append("]");
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed to extract getServerErrCode from {}", cls.getName(), e);
        }
        try {
            var lscMethod = cls.getMethod("getLegacyServerCode");
            int legacyServerCode = (int) lscMethod.invoke(ex);
            if (legacyServerCode != 0) {
                sb.append(" [legacyServerCode=").append(legacyServerCode).append("]");
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.debug("Failed to extract getLegacyServerCode from {}", cls.getName(), e);
        }
    }

    private String safeDatabaseName(String databaseName) {
        return (databaseName == null || databaseName.isBlank()) ? "default" : databaseName.trim();
    }

    public Map<String, Object> createCollection(MilvusCreateCollectionRequest request) throws InterruptedException {
        String databaseName = request.databaseNameOrDefault();
        String collectionName = safeCollectionName(request.collectionName());
        assertTenantCanAccessCollection(collectionName);
        DataType primaryIdType = parseIdType(request.idType());

        // 先切换当前连接所使用的数据库
        milvusClient.useDatabase(databaseName);

        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .description(request.description() == null ? "" : request.description())
                .dimension(request.dimension())
                .primaryFieldName(request.primaryFieldNameOrDefault())
                .idType(primaryIdType)
                .maxLength(primaryIdType == DataType.VarChar ? request.maxLengthOrDefault() : null)
                .vectorFieldName(request.vectorFieldNameOrDefault())
                .metricType(request.metricTypeOrDefault())
                .autoID(request.autoIdOrDefault())
                .enableDynamicField(request.enableDynamicFieldOrDefault())
                .numShards(request.numShardsOrDefault())
                .build();

        milvusClient.createCollection(createReq);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("databaseName", databaseName);
        result.put("collectionName", collectionName);
        result.put("primaryFieldName", request.primaryFieldNameOrDefault());
        result.put("idType", primaryIdType.name());
        result.put("vectorFieldName", request.vectorFieldNameOrDefault());
        result.put("dimension", request.dimension());
        result.put("metricType", request.metricTypeOrDefault());
        result.put("autoId", request.autoIdOrDefault());
        result.put("enableDynamicField", request.enableDynamicFieldOrDefault());
        result.put("numShards", request.numShardsOrDefault());
        return result;
    }

    private void assertTenantCanAccessCollection(String collectionName) {
        if (!tenantCanAccessCollection(collectionName)) {
            throw new IllegalArgumentException("collectionName is not accessible for current tenant: " + collectionName);
        }
    }

    private boolean tenantCanAccessCollection(String collectionName) {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(null);
        if (tenantId == null || tenantId <= 0) {
            return true;
        }
        return collectionResolver.belongsToTenant(collectionName, tenantId);
    }

    private DataType parseIdType(String idType) {
        if (idType == null || idType.isBlank()) {
            return DataType.Int64;
        }
        return switch (idType.trim().toUpperCase()) {
            case "INT64" -> DataType.Int64;
            case "VARCHAR", "STRING", "VAR_CHAR" -> DataType.VarChar;
            default -> throw new IllegalArgumentException("idType 仅支持 INT64 或 VARCHAR");
        };
    }

    private String safeCollectionName(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("collectionName must not be blank");
        }
        String trimmed = collectionName.trim();
        if ("undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            throw new IllegalArgumentException("collectionName is invalid: " + trimmed);
        }
        return trimmed;
    }
}

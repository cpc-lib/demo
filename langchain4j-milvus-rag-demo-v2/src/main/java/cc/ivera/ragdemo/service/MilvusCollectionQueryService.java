package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.controller.MilvusCollectionQueryRequest;
import cc.ivera.ragdemo.controller.MilvusCreateCollectionRequest;
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
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MilvusCollectionQueryService {

    private final MilvusClientV2 milvusClient;
    private final ObjectMapper objectMapper;

    public List<String> listCollections(String databaseName) throws InterruptedException {
        milvusClient.useDatabase(safeDatabaseName(databaseName));
        return milvusClient.listCollections().getCollectionNames();
    }

    public Map<String, Object> describeCollection(String databaseName, String collectionName) throws InterruptedException {
        milvusClient.useDatabase(safeDatabaseName(databaseName));
        var resp = milvusClient.describeCollection(
                DescribeCollectionReq.builder()
                        .collectionName(collectionName)
                        .build()
        );
        return objectMapper.convertValue(resp, new TypeReference<>() {});
    }

    public Map<String, Object> query(MilvusCollectionQueryRequest request) throws InterruptedException {
        milvusClient.useDatabase(request.safeDatabaseName());

        if (request.loadBeforeQuery()) {
            milvusClient.loadCollection(
                    LoadCollectionReq.builder()
                            .collectionName(request.collectionName())
                            .build()
            );
        }

        QueryReq queryReq = QueryReq.builder()
                .collectionName(request.collectionName())
                .filter(request.safeFilter())
                .outputFields(request.outputFields())
                .partitionNames(request.partitionNames())
                .offset(request.offset())
                .limit(request.safeLimit())
                .build();

        QueryResp queryResp = milvusClient.query(queryReq);
        Map<String, Object> raw = objectMapper.convertValue(queryResp, new TypeReference<>() {});

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("databaseName", request.safeDatabaseName());
        result.put("collectionName", request.collectionName());
        result.put("filter", request.safeFilter());
        result.put("offset", request.offset());
        result.put("limit", request.safeLimit());
        result.put("rows", raw.getOrDefault("queryResults", List.of()));
        result.put("raw", raw);
        return result;
    }

    private String safeDatabaseName(String databaseName) {
        return (databaseName == null || databaseName.isBlank()) ? "default" : databaseName.trim();
    }

    public Map<String, Object> createCollection(MilvusCreateCollectionRequest request) throws InterruptedException {
        String databaseName = request.databaseNameOrDefault();
        DataType primaryIdType = parseIdType(request.idType());

        // 先切换当前连接所使用的数据库
        milvusClient.useDatabase(databaseName);

        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(request.collectionName())
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
        result.put("collectionName", request.collectionName());
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
}

package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MilvusTenantDeletionWorker implements TenantDeletionWorker {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final RagProperties properties;
    private final RagDocumentChunkMapper chunkMapper;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final ObjectProvider<MultimodalVectorStore> multimodalVectorStore;
    private final ObjectMapper objectMapper;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.MILVUS.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        VectorScope scope = vectorScope(task.getTenantId());
        return TenantDeletionStageResult.success(stageCode(), scope.totalCount(), scope.detailJson());
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        if (!properties.getTenantDeletion().isMilvusDeleteEnabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "milvus-delete-disabled");
        }
        VectorScope scope = vectorScope(task.getTenantId());
        for (Map.Entry<VectorStoreKey, List<String>> entry : scope.textVectorIdsByStore().entrySet()) {
            List<String> ids = entry.getValue().stream().distinct().toList();
            if (!ids.isEmpty()) {
                VectorStoreKey store = entry.getKey();
                dynamicMilvusStoreManager.context(store.alias(), store.collection()).store().removeAll(ids);
            }
        }
        MultimodalVectorStore multimodal = multimodalVectorStore.getIfAvailable();
        if (multimodal != null && !scope.imageVectorIds().isEmpty()) {
            multimodal.deleteByIds(task.getTenantId(), scope.imageVectorIds().stream().distinct().toList());
        }
        return TenantDeletionStageResult.success(stageCode(), scope.totalCount(), scope.detailJson());
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        VectorScope scope = vectorScope(task.getTenantId());
        boolean metadataRemain = scope.totalCount() > 0 && properties.getTenantDeletion().isMysqlDeleteEnabled();
        return metadataRemain
                ? TenantDeletionStageResult.failed(stageCode(), "VECTOR_METADATA_REMAIN", "Remaining vector metadata rows: " + scope.totalCount())
                : TenantDeletionStageResult.success(stageCode(), scope.totalCount(), scope.detailJson());
    }

    private VectorScope vectorScope(Long tenantId) {
        // The tenant being deleted (tenantId param) may differ from the platform admin's own
        // tenant context. Bypass the interceptor so chunks are filtered solely by the target
        // tenant, not additionally by the caller's tenant_id.
        List<RagDocumentChunk> chunks = TenantContextHolder.callWithBypass(() -> chunkMapper.selectList(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getTenantId, tenantId)
                .eq(RagDocumentChunk::getIsDeleted, 0)));
        Map<VectorStoreKey, List<String>> textByStore = new LinkedHashMap<>();
        Set<String> imageIds = new LinkedHashSet<>();
        for (RagDocumentChunk chunk : chunks) {
            String alias = StringUtils.hasText(chunk.getMilvusAlias()) ? chunk.getMilvusAlias() : dynamicMilvusStoreManager.currentAlias();
            String collection = StringUtils.hasText(chunk.getVectorCollection()) ? chunk.getVectorCollection() : fallbackCollection(alias);
            List<String> textIds = new ArrayList<>();
            if (StringUtils.hasText(chunk.getVectorId())) {
                textIds.add(chunk.getVectorId());
            }
            textIds.addAll(readIds(chunk.getTextVectorIds()));
            if (!textIds.isEmpty()) {
                textByStore.computeIfAbsent(new VectorStoreKey(alias, collection), ignored -> new ArrayList<>()).addAll(textIds);
            }
            imageIds.addAll(readIds(chunk.getImageVectorIds()));
        }
        return new VectorScope(textByStore, imageIds.stream().toList());
    }

    private String fallbackCollection(String alias) {
        try {
            return dynamicMilvusStoreManager.load(alias).getCollection();
        } catch (Exception ex) {
            log.warn("Failed to load Milvus collection for alias={}, falling back to configured default: {}",
                    alias, ex.getMessage());
            return properties.getMilvus().getCollection();
        }
    }

    private List<String> readIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST).stream().filter(StringUtils::hasText).toList();
        } catch (Exception e) {
            log.warn("Failed to parse ID list JSON, returning empty list: {}", e.getMessage());
            return List.of();
        }
    }

    private record VectorStoreKey(String alias, String collection) {
    }

    private record VectorScope(Map<VectorStoreKey, List<String>> textVectorIdsByStore, List<String> imageVectorIds) {
        long totalCount() {
            return textVectorIdsByStore.values().stream().mapToLong(List::size).sum() + imageVectorIds.size();
        }

        String detailJson() {
            return "{\"textVectorStores\":" + textVectorIdsByStore.keySet().size()
                    + ",\"textVectors\":" + textVectorIdsByStore.values().stream().mapToLong(List::size).sum()
                    + ",\"imageVectors\":" + imageVectorIds.size() + "}";
        }
    }
}

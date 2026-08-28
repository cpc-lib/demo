package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.model.knowledge.MultimodalCollectionStatus;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorRecord;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchHit;
import cc.ivera.ragdemo.model.knowledge.MultimodalVectorSearchRequest;

import java.util.List;

public interface MultimodalVectorStore {

    boolean enabled();

    MultimodalCollectionStatus ensureCollection();

    List<String> fieldNames();

    String upsert(MultimodalVectorRecord record);

    List<MultimodalVectorSearchHit> search(MultimodalVectorSearchRequest request);

    void deleteByIds(List<String> ids);

    default void deleteByIds(Long tenantId, List<String> ids) {
        deleteByIds(ids);
    }

    void deleteByChunkUid(String chunkUid);

    default void deleteByChunkUid(Long tenantId, String chunkUid) {
        deleteByChunkUid(chunkUid);
    }
}

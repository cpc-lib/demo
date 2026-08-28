package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;

import java.util.List;

public interface KeywordSearchIndex {

    boolean enabled();

    List<RagSearchItem> search(RagRetrievalCriteria criteria, int candidateLimit);

    void upsert(RagDocumentChunk chunk);

    void upsert(KnowledgeChunkRecord record);

    void delete(String chunkId);
}

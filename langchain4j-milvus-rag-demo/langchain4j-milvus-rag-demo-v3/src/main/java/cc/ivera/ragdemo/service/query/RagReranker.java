package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;

import java.util.List;

public interface RagReranker {

    List<RagSearchItem> rerank(RagRetrievalCriteria criteria,
                               List<RagRetrievalResultSet> resultSets,
                               int topK);
}

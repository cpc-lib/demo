package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagSearchItem;

import java.util.List;

public record RagRetrievalResultSet(
        String source,
        double weight,
        List<RagSearchItem> items
) {
}

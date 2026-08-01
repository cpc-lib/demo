package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagSearchResponse(
        Long queryLogId,
        List<RagSearchItem> items
) {
}

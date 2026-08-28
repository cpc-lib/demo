package cc.ivera.ragdemo.model.query;

import java.time.LocalDateTime;
import java.util.List;

public record RagQueryLogOperationRequest(
        List<Long> ids,
        Long tenantId,
        String queryType,
        String status,
        String conversationId,
        String traceId,
        String queryText,
        String operator,
        String reason,
        LocalDateTime retentionUntil
) {
}

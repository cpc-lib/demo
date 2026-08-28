package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagQueryLogOperationResponse(
        String deleteNo,
        String mode,
        int matchedCount,
        int successCount,
        int failedCount,
        List<Long> queryLogIds
) {
}

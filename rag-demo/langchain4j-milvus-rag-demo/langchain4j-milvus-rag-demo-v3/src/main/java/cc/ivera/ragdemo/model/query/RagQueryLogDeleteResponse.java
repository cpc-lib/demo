package cc.ivera.ragdemo.model.query;

public record RagQueryLogDeleteResponse(
        int requested,
        int deletedLogs,
        int deletedHits,
        int deletedFeedbacks
) {
}

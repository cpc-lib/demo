package cc.ivera.ragdemo.model.knowledge;

public interface KnowledgeIngestionProgressListener {

    KnowledgeIngestionProgressListener NOOP = new KnowledgeIngestionProgressListener() {
    };

    default void checkCancelled() {
    }

    default void stageStarted(String stageCode) {
    }

    default void stageProgress(String stageCode, int successCount, int failedCount, int totalCount) {
    }

    default void stageCompleted(String stageCode, int successCount, int failedCount, int totalCount) {
    }

    default void stageSkipped(String stageCode, String message) {
    }
}

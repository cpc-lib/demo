package cc.ivera.ragdemo.domain.rag;

public enum IngestionShardStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    CANCELLED
}

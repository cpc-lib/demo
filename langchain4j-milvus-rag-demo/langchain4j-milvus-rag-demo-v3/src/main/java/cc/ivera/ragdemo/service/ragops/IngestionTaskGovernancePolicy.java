package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;

public final class IngestionTaskGovernancePolicy {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private IngestionTaskGovernancePolicy() {
    }

    public static boolean canCancel(IngestionTaskStatus status) {
        return IngestionTaskStateMachine.canTransit(status, IngestionTaskStatus.CANCELLED);
    }

    public static void assertCanCancel(IngestionTaskStatus status) {
        IngestionTaskStateMachine.assertTransit(status, IngestionTaskStatus.CANCELLED);
    }

    public static boolean canRetry(IngestionTaskStatus status, Integer retryCount, Integer maxRetryCount) {
        return (status == IngestionTaskStatus.FAILED || status == IngestionTaskStatus.PARTIAL_SUCCESS)
                && safeRetryCount(retryCount) < safeMaxRetryCount(maxRetryCount);
    }

    public static void assertCanRetry(IngestionTaskStatus status, Integer retryCount, Integer maxRetryCount) {
        if (status != IngestionTaskStatus.FAILED && status != IngestionTaskStatus.PARTIAL_SUCCESS) {
            throw new IllegalStateException("Task status does not allow retry: " + status);
        }
        if (!canRetry(status, retryCount, maxRetryCount)) {
            throw new IllegalStateException("Retry budget exhausted: " + safeRetryCount(retryCount)
                    + "/" + safeMaxRetryCount(maxRetryCount));
        }
    }

    public static int nextRetryCount(Integer retryCount) {
        return safeRetryCount(retryCount) + 1;
    }

    private static int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : Math.max(0, retryCount);
    }

    private static int safeMaxRetryCount(Integer maxRetryCount) {
        return maxRetryCount == null ? DEFAULT_MAX_RETRY_COUNT : Math.max(0, maxRetryCount);
    }
}

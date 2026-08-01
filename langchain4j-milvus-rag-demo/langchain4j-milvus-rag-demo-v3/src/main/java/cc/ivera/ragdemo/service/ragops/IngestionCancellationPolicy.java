package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;

public final class IngestionCancellationPolicy {

    private IngestionCancellationPolicy() {
    }

    public static CancelDecision decide(IngestionTaskStatus status) {
        if (status == IngestionTaskStatus.RUNNING) {
            return CancelDecision.REQUEST_CANCEL;
        }
        if (status == IngestionTaskStatus.PENDING
                || status == IngestionTaskStatus.RETRY_WAIT
                || status == IngestionTaskStatus.FAILED) {
            return CancelDecision.DIRECT_CANCEL;
        }
        return CancelDecision.REJECT;
    }

    public static void assertCanCancel(IngestionTaskStatus status) {
        if (decide(status) == CancelDecision.REJECT) {
            throw new IllegalStateException("Task status does not allow cancellation: " + status);
        }
    }

    public enum CancelDecision {
        DIRECT_CANCEL,
        REQUEST_CANCEL,
        REJECT
    }
}

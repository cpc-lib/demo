package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;

import java.util.Map;
import java.util.Set;

public final class IngestionTaskStateMachine {

    private static final Map<IngestionTaskStatus, Set<IngestionTaskStatus>> TRANSITIONS = Map.of(
            IngestionTaskStatus.PENDING, Set.of(IngestionTaskStatus.RUNNING, IngestionTaskStatus.CANCELLED),
            IngestionTaskStatus.RUNNING, Set.of(IngestionTaskStatus.SUCCESS, IngestionTaskStatus.PARTIAL_SUCCESS, IngestionTaskStatus.FAILED, IngestionTaskStatus.CANCELLED),
            IngestionTaskStatus.FAILED, Set.of(IngestionTaskStatus.RETRY_WAIT, IngestionTaskStatus.CANCELLED),
            IngestionTaskStatus.PARTIAL_SUCCESS, Set.of(IngestionTaskStatus.RETRY_WAIT),
            IngestionTaskStatus.RETRY_WAIT, Set.of(IngestionTaskStatus.RUNNING, IngestionTaskStatus.CANCELLED)
    );

    private IngestionTaskStateMachine() {
    }

    public static boolean canTransit(IngestionTaskStatus from, IngestionTaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransit(IngestionTaskStatus from, IngestionTaskStatus to) {
        if (!canTransit(from, to)) {
            throw new IllegalStateException("Illegal ingestion task transition: " + from + " -> " + to);
        }
    }
}

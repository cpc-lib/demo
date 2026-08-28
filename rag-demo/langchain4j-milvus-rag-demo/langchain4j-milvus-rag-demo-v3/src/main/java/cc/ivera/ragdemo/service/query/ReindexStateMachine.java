package cc.ivera.ragdemo.service.query;

import java.util.Map;
import java.util.Set;

public class ReindexStateMachine {

    public enum ReindexJobStatus {
        PLANNED,
        CREATING_INDEX,
        BACKFILLING,
        VALIDATING,
        READY_TO_SWITCH,
        SWITCHING_ALIAS,
        SUCCEEDED,
        FAILED,
        ROLLING_BACK,
        ROLLED_BACK
    }

    private static final Map<ReindexJobStatus, Set<ReindexJobStatus>> TRANSITIONS = Map.of(
            ReindexJobStatus.PLANNED, Set.of(ReindexJobStatus.CREATING_INDEX, ReindexJobStatus.FAILED),
            ReindexJobStatus.CREATING_INDEX, Set.of(ReindexJobStatus.BACKFILLING, ReindexJobStatus.FAILED),
            ReindexJobStatus.BACKFILLING, Set.of(ReindexJobStatus.VALIDATING, ReindexJobStatus.FAILED),
            ReindexJobStatus.VALIDATING, Set.of(ReindexJobStatus.READY_TO_SWITCH, ReindexJobStatus.FAILED),
            ReindexJobStatus.READY_TO_SWITCH, Set.of(ReindexJobStatus.SWITCHING_ALIAS, ReindexJobStatus.FAILED),
            ReindexJobStatus.SWITCHING_ALIAS, Set.of(ReindexJobStatus.SUCCEEDED, ReindexJobStatus.FAILED),
            ReindexJobStatus.SUCCEEDED, Set.of(ReindexJobStatus.ROLLING_BACK, ReindexJobStatus.FAILED),
            ReindexJobStatus.FAILED, Set.of(ReindexJobStatus.ROLLING_BACK),
            ReindexJobStatus.ROLLING_BACK, Set.of(ReindexJobStatus.ROLLED_BACK, ReindexJobStatus.FAILED),
            ReindexJobStatus.ROLLED_BACK, Set.of()
    );

    public void assertTransition(ReindexJobStatus from, ReindexJobStatus to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("Invalid reindex transition: " + from + " -> " + to);
        }
    }

    public boolean isTerminal(ReindexJobStatus status) {
        return TRANSITIONS.getOrDefault(status, Set.of()).isEmpty();
    }
}

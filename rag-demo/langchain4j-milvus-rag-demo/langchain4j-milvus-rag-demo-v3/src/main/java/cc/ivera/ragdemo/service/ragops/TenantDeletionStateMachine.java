package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.admin.TenantDeletionExecutionMode;
import cc.ivera.ragdemo.admin.TenantDeletionTaskStatus;

import java.util.Map;
import java.util.Set;

public class TenantDeletionStateMachine {

    private static final Map<TenantDeletionTaskStatus, Set<TenantDeletionTaskStatus>> TRANSITIONS = Map.of(
            TenantDeletionTaskStatus.PENDING, Set.of(TenantDeletionTaskStatus.RUNNING, TenantDeletionTaskStatus.CANCELLED),
            TenantDeletionTaskStatus.RUNNING, Set.of(TenantDeletionTaskStatus.VERIFYING, TenantDeletionTaskStatus.PARTIAL_FAILED, TenantDeletionTaskStatus.FAILED, TenantDeletionTaskStatus.CANCEL_REQUESTED),
            TenantDeletionTaskStatus.VERIFYING, Set.of(TenantDeletionTaskStatus.SUCCEEDED, TenantDeletionTaskStatus.PARTIAL_FAILED, TenantDeletionTaskStatus.FAILED),
            TenantDeletionTaskStatus.PARTIAL_FAILED, Set.of(TenantDeletionTaskStatus.RUNNING, TenantDeletionTaskStatus.FAILED),
            TenantDeletionTaskStatus.FAILED, Set.of(TenantDeletionTaskStatus.RUNNING),
            TenantDeletionTaskStatus.CANCEL_REQUESTED, Set.of(TenantDeletionTaskStatus.CANCELLED, TenantDeletionTaskStatus.FAILED),
            TenantDeletionTaskStatus.CANCELLED, Set.of(TenantDeletionTaskStatus.RUNNING),
            TenantDeletionTaskStatus.SUCCEEDED, Set.of()
    );

    public void assertTransition(TenantDeletionTaskStatus current, TenantDeletionTaskStatus next) {
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Invalid tenant deletion transition: " + current + " -> " + next);
        }
    }

    public void assertExecutionAllowed(TenantDeletionExecutionMode mode, boolean executeEnabled) {
        if (mode == TenantDeletionExecutionMode.EXECUTE && !executeEnabled) {
            throw new IllegalStateException("Tenant deletion execute mode is disabled; run DRY_RUN or enable explicit execution");
        }
    }
}

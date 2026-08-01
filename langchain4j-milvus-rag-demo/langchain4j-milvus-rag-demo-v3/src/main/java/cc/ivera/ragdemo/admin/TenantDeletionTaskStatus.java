package cc.ivera.ragdemo.admin;

public enum TenantDeletionTaskStatus {
    PENDING,
    RUNNING,
    VERIFYING,
    SUCCEEDED,
    PARTIAL_FAILED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED
}

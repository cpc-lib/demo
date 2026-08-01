package cc.ivera.ragdemo.admin;

public record TenantDeletionTaskRequest(
        String reason,
        String executionMode
) {
}

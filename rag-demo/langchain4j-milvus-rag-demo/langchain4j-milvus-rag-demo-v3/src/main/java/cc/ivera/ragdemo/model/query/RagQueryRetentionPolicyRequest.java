package cc.ivera.ragdemo.model.query;

public record RagQueryRetentionPolicyRequest(
        Long tenantId,
        String policyName,
        String queryType,
        String statusFilter,
        Integer retentionDays,
        Boolean archiveBeforeDelete,
        Boolean enabled
) {
}

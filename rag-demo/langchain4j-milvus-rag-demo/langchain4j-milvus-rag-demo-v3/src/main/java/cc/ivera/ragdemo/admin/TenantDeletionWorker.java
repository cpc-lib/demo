package cc.ivera.ragdemo.admin;

import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;

public interface TenantDeletionWorker {

    String stageCode();

    TenantDeletionStageResult dryRun(TenantDataDeletionTask task);

    TenantDeletionStageResult execute(TenantDataDeletionTask task);

    TenantDeletionStageResult verify(TenantDataDeletionTask task);
}

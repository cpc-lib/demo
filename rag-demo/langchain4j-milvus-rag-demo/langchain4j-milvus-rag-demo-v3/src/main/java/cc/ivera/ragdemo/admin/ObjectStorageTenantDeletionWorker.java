package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.service.ragops.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ObjectStorageTenantDeletionWorker implements TenantDeletionWorker {

    private final RagProperties properties;
    private final ObjectStorageService objectStorageService;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.OBJECT_STORAGE.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        long count = objectStorageService.countTenantObjects(properties.getTenant().getObjectEnvironment(), task.getTenantId());
        return TenantDeletionStageResult.success(stageCode(), count, "{\"objects\":" + count + "}");
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        if (!properties.getTenantDeletion().isObjectStorageDeleteEnabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "object-storage-delete-disabled");
        }
        long deleted = objectStorageService.deleteTenantObjects(properties.getTenant().getObjectEnvironment(), task.getTenantId());
        return TenantDeletionStageResult.success(stageCode(), deleted, "{\"deletedObjects\":" + deleted + "}");
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        long remaining = objectStorageService.countTenantObjects(properties.getTenant().getObjectEnvironment(), task.getTenantId());
        return remaining == 0 || !properties.getTenantDeletion().isObjectStorageDeleteEnabled()
                ? TenantDeletionStageResult.success(stageCode(), remaining, "{\"remainingObjects\":" + remaining + "}")
                : TenantDeletionStageResult.failed(stageCode(), "OBJECTS_REMAIN", "Remaining tenant objects: " + remaining);
    }
}

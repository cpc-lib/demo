package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.SysOperationAuditLogMapper;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AuditRetentionTenantDeletionWorker implements TenantDeletionWorker {

    private final SysOperationAuditLogMapper auditLogMapper;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.AUDIT_RETENTION.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        // Deletion workers operate on the target tenant (task.getTenantId()), which may differ
        // from the platform admin's own tenant context. Bypass the tenant interceptor so the
        // count reflects ALL audit rows targeting the deleted tenant across operator tenants.
        Long count = TenantContextHolder.callWithBypass(() -> auditLogMapper.selectCount(new LambdaQueryWrapper<cc.ivera.ragdemo.domain.tenant.SysOperationAuditLog>()
                .eq(cc.ivera.ragdemo.domain.tenant.SysOperationAuditLog::getTargetTenantId, task.getTenantId())));
        return TenantDeletionStageResult.success(stageCode(), count == null ? 0 : count,
                "{\"retainedAuditRows\":" + (count == null ? 0 : count) + ",\"policy\":\"retain\"}");
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        return dryRun(task);
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        return dryRun(task);
    }
}

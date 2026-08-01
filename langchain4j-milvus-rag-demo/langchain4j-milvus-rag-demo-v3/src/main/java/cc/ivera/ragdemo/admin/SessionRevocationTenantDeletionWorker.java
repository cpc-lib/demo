package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.SysAdminImpersonationSessionMapper;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SessionRevocationTenantDeletionWorker implements TenantDeletionWorker {

    private final SysAdminImpersonationSessionMapper impersonationSessionMapper;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.SESSION.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        // Sessions target the tenant being deleted; they may have been created by operators
        // from different tenants. Bypass the interceptor to count/revoked ALL sessions
        // targeting task.getTenantId() regardless of the operator's tenant_id.
        Long count = TenantContextHolder.callWithBypass(() -> impersonationSessionMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession>()
                .eq(cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession::getTargetTenantId, task.getTenantId())
                .isNull(cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession::getRevokedAt)));
        return TenantDeletionStageResult.success(stageCode(), count == null ? 0 : count, "{\"activeImpersonationSessions\":" + (count == null ? 0 : count) + "}");
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        int updated = TenantContextHolder.callWithBypass(() -> impersonationSessionMapper.update(null, new LambdaUpdateWrapper<cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession>()
                .eq(cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession::getTargetTenantId, task.getTenantId())
                .isNull(cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession::getRevokedAt)
                .set(cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession::getRevokedAt, LocalDateTime.now())));
        return TenantDeletionStageResult.success(stageCode(), updated, "{\"revokedSessions\":" + updated + "}");
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        return dryRun(task);
    }
}

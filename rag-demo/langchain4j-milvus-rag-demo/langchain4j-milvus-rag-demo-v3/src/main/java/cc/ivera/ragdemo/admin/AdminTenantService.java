package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.SysAdminImpersonationSession;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionStage;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.SysAdminImpersonationSessionMapper;
import cc.ivera.ragdemo.mapper.TenantDataDeletionStageMapper;
import cc.ivera.ragdemo.mapper.TenantDataDeletionTaskMapper;
import cc.ivera.ragdemo.service.ragops.AdminImpersonationPolicy;
import cc.ivera.ragdemo.service.ragops.AdminRolePolicy;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AdminTenantService {

    private final SysAdminImpersonationSessionMapper impersonationMapper;
    private final TenantDataDeletionTaskMapper deletionTaskMapper;
    private final TenantDataDeletionStageMapper deletionStageMapper;
    private final TenantAuditService auditService;
    private final TenantDeletionExecutor deletionExecutor;
    private final RagProperties properties;
    private final AdminRolePolicy adminRolePolicy;
    private final AdminImpersonationPolicy policy = new AdminImpersonationPolicy();

    public SysAdminImpersonationSession startImpersonation(AdminImpersonationRequest request) {
        TenantContext context = TenantContextHolder.require();
        policy.assertCanStart(context.user(), context.tenantId(), request.targetTenantId(), request.reason(), adminRoles());
        SysAdminImpersonationSession session = new SysAdminImpersonationSession();
        session.setSessionNo("imp-" + UUID.randomUUID().toString().replace("-", ""));
        session.setOperatorUserId(context.operatorUserId());
        session.setOperatorTenantId(context.tenantId());
        session.setTargetTenantId(request.targetTenantId());
        session.setImpersonationReason(request.reason().trim());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(request.ttlMinutes() == null ? 30 : Math.max(1, request.ttlMinutes())));
        session.setCreatedAt(LocalDateTime.now());
        impersonationMapper.insert(session);
        auditService.record("ADMIN_IMPERSONATION_START", "TENANT", String.valueOf(request.targetTenantId()), "SUCCESS", "{}");
        return session;
    }

    public void revokeCurrentImpersonation() {
        TenantContext context = TenantContextHolder.require();
        // Find and revoke the active impersonation session for this operator.
        // Use bypass because the session's tenant_id (operator's) may differ from the
        // impersonated target tenant in the current context.
        String operatorUserId = context.operatorUserId();
        Long operatorTenantId = context.operatorTenantId() != null ? context.operatorTenantId() : context.tenantId();
        TenantContextHolder.callWithBypass(() -> {
            SysAdminImpersonationSession active = impersonationMapper.selectOne(new LambdaQueryWrapper<SysAdminImpersonationSession>()
                    .eq(SysAdminImpersonationSession::getOperatorUserId, operatorUserId)
                    .eq(SysAdminImpersonationSession::getOperatorTenantId, operatorTenantId)
                    .isNull(SysAdminImpersonationSession::getRevokedAt)
                    .gt(SysAdminImpersonationSession::getExpiresAt, LocalDateTime.now())
                    .last("LIMIT 1"));
            if (active != null) {
                impersonationMapper.update(null, new LambdaUpdateWrapper<SysAdminImpersonationSession>()
                        .eq(SysAdminImpersonationSession::getId, active.getId())
                        .isNull(SysAdminImpersonationSession::getRevokedAt)
                        .set(SysAdminImpersonationSession::getRevokedAt, LocalDateTime.now()));
            }
            return null;
        });
        auditService.record("ADMIN_IMPERSONATION_REVOKE", "TENANT", String.valueOf(context.tenantId()), "SUCCESS", "{}");
    }

    public TenantDataDeletionTask createDeletionTask(Long tenantId, TenantDeletionTaskRequest request) {
        TenantContext context = TenantContextHolder.require();
        if (!platformAdmin(context)) {
            throw new IllegalStateException("Only platform administrators can create tenant deletion tasks");
        }
        TenantDataDeletionTask task = new TenantDataDeletionTask();
        task.setTaskNo("tdel-" + UUID.randomUUID().toString().replace("-", ""));
        task.setTenantId(tenantId);
        task.setRequestedBy(context.operatorUserId());
        task.setReason(request == null ? null : request.reason());
        task.setExecutionMode(TenantDeletionExecutionMode.normalize(request == null ? null : request.executionMode()).name());
        task.setTaskStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        deletionTaskMapper.insert(task);
        for (String stage : TenantDeletionStageCode.defaultStageCodes()) {
            TenantDataDeletionStage row = new TenantDataDeletionStage();
            row.setTaskId(task.getId());
            row.setStageCode(stage);
            row.setStageStatus("PENDING");
            row.setDeletedCount(0L);
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            deletionStageMapper.insert(row);
        }
        auditService.record("TENANT_DELETION_TASK_CREATE", "TENANT", String.valueOf(tenantId), "SUCCESS", "{}");
        return task;
    }

    public Object deletionTask(Long taskId) {
        return deletionExecutor.detail(taskId);
    }

    public Object runDeletionTask(Long taskId, String executionMode) {
        TenantContext context = TenantContextHolder.require();
        if (!platformAdmin(context)) {
            throw new IllegalStateException("Only platform administrators can run tenant deletion tasks");
        }
        Object result = deletionExecutor.run(taskId, executionMode);
        auditService.record("TENANT_DELETION_TASK_RUN", "TENANT_DELETION_TASK", String.valueOf(taskId), "SUCCESS", "{}");
        return result;
    }

    public Object cancelDeletionTask(Long taskId) {
        TenantContext context = TenantContextHolder.require();
        if (!platformAdmin(context)) {
            throw new IllegalStateException("Only platform administrators can cancel tenant deletion tasks");
        }
        Object result = deletionExecutor.cancel(taskId);
        auditService.record("TENANT_DELETION_TASK_CANCEL", "TENANT_DELETION_TASK", String.valueOf(taskId), "SUCCESS", "{}");
        return result;
    }

    public Object retryDeletionStage(Long taskId, String stageCode, String executionMode) {
        TenantContext context = TenantContextHolder.require();
        if (!platformAdmin(context)) {
            throw new IllegalStateException("Only platform administrators can retry tenant deletion stages");
        }
        Object result = deletionExecutor.retryStage(taskId, stageCode, executionMode);
        auditService.record("TENANT_DELETION_STAGE_RETRY", "TENANT_DELETION_TASK", String.valueOf(taskId), "SUCCESS",
                "{\"stageCode\":\"" + stageCode + "\"}");
        return result;
    }

    private boolean platformAdmin(TenantContext context) {
        return context != null && adminRolePolicy.isPlatformAdmin(context.user(), adminRoles());
    }

    private java.util.List<String> adminRoles() {
        return properties.getSecurity().getAdminRoles();
    }
}

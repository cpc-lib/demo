package cc.ivera.ragdemo.audit;


import cc.ivera.ragdemo.domain.tenant.SysOperationAuditLog;
import cc.ivera.ragdemo.mapper.SysOperationAuditLogMapper;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantAuditService {

    private final SysOperationAuditLogMapper mapper;

    public void record(String operation, String resourceType, String resourceId, String result, String detailJson) {
        try {
            TenantContext context = TenantContextHolder.current().orElse(null);
            SysOperationAuditLog logRow = new SysOperationAuditLog();
            logRow.setOperatorUserId(context == null ? "unknown" : context.operatorUserId());
            logRow.setOperatorTenantId(context == null ? null : context.operatorTenantId());
            logRow.setTargetTenantId(context == null ? null : context.tenantId());
            logRow.setImpersonationReason(context == null ? null : context.impersonationReason());
            logRow.setRequestId(context == null ? null : context.requestId());
            logRow.setSourceIp(context == null ? null : context.sourceIp());
            logRow.setOperation(operation);
            logRow.setResourceType(resourceType);
            logRow.setResourceId(resourceId);
            logRow.setResult(result == null ? "SUCCESS" : result);
            logRow.setDetailJson(detailJson == null ? "{}" : detailJson);
            logRow.setCreatedAt(LocalDateTime.now());
            mapper.insert(logRow);
        } catch (Exception ex) {
            log.warn("Failed to write tenant operation audit log: operation={}, resourceType={}, resourceId={}",
                    operation, resourceType, resourceId, ex);
        }
    }

    public Object page(String operation, String resourceType, Long targetTenantId, int limit) {
        // Audit log viewing is a cross-tenant admin operation: the page query filters by
        // targetTenantId (optional) and should not be restricted to the caller's own tenant_id.
        return TenantContextHolder.callWithBypass(() -> mapper.selectList(new LambdaQueryWrapper<SysOperationAuditLog>()
                .eq(operation != null && !operation.isBlank(), SysOperationAuditLog::getOperation, operation)
                .eq(resourceType != null && !resourceType.isBlank(), SysOperationAuditLog::getResourceType, resourceType)
                .eq(targetTenantId != null, SysOperationAuditLog::getTargetTenantId, targetTenantId)
                .orderByDesc(SysOperationAuditLog::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(500, limit)))));
    }

    public void recordSuccess(String operation, String resourceType, Object resourceId) {
        record(operation, resourceType, resourceId == null ? null : String.valueOf(resourceId), "SUCCESS", "{}");
    }
}

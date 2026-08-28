package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.admin.AdminImpersonationRequest;
import cc.ivera.ragdemo.admin.AdminTenantService;
import cc.ivera.ragdemo.admin.TenantDeletionTaskRequest;
import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.domain.tenant.RagTenantQuota;
import cc.ivera.ragdemo.model.dto.EntityDtoConverter;
import cc.ivera.ragdemo.model.dto.RagTenantQuotaDto;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.quota.TenantQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "Tenant Admin", description = "Tenant impersonation, audit, quota and deletion ledgers")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;
    private final TenantAuditService auditService;
    private final TenantQuotaService quotaService;
    private final EntityDtoConverter converter;

    @PostMapping("/impersonations")
    @Operation(summary = "Start explicit tenant impersonation")
    public RagApiResponse<?> startImpersonation(@RequestBody AdminImpersonationRequest request) {
        return RagApiResponse.ok(adminTenantService.startImpersonation(request));
    }

    @DeleteMapping("/impersonations/current")
    @Operation(summary = "Revoke current tenant impersonation")
    public RagApiResponse<?> revokeCurrentImpersonation() {
        adminTenantService.revokeCurrentImpersonation();
        return RagApiResponse.ok(java.util.Map.of("revoked", true));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List operation audit logs")
    public RagApiResponse<?> auditLogs(@RequestParam(value = "operation", required = false) String operation,
                                       @RequestParam(value = "resourceType", required = false) String resourceType,
                                       @RequestParam(value = "targetTenantId", required = false) Long targetTenantId,
                                       @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit) {
        return RagApiResponse.ok(auditService.page(operation, resourceType, targetTenantId, limit));
    }

    @GetMapping("/tenants/{tenantId}/quota")
    @Operation(summary = "Get tenant quota")
    public RagApiResponse<RagTenantQuotaDto> quota(@PathVariable Long tenantId) {
        return RagApiResponse.ok(converter.toDto(quotaService.getQuota(tenantId)));
    }

    @PutMapping("/tenants/{tenantId}/quota")
    @Operation(summary = "Update tenant quota")
    public RagApiResponse<RagTenantQuotaDto> updateQuota(@PathVariable Long tenantId,
                                                      @RequestBody RagTenantQuota request) {
        return RagApiResponse.ok(converter.toDto(quotaService.updateQuota(tenantId, request)));
    }

    @GetMapping("/tenants/{tenantId}/usage")
    @Operation(summary = "Get tenant daily usage")
    public RagApiResponse<?> usage(@PathVariable Long tenantId,
                                   @RequestParam(value = "date", required = false) String date) {
        return RagApiResponse.ok(quotaService.getUsage(tenantId, date == null ? null : LocalDate.parse(date)));
    }

    @PostMapping("/tenants/{tenantId}/deletion-tasks")
    @Operation(summary = "Create tenant data deletion task")
    public RagApiResponse<?> createDeletionTask(@PathVariable Long tenantId,
                                                @RequestBody(required = false) TenantDeletionTaskRequest request) {
        return RagApiResponse.ok(adminTenantService.createDeletionTask(tenantId, request));
    }

    @GetMapping("/tenant-deletion-tasks/{taskId}")
    @Operation(summary = "Get tenant data deletion task")
    public RagApiResponse<?> deletionTask(@PathVariable Long taskId) {
        return RagApiResponse.ok(adminTenantService.deletionTask(taskId));
    }

    @PostMapping("/tenant-deletion-tasks/{taskId}/run")
    @Operation(summary = "Run tenant data deletion task")
    public RagApiResponse<?> runDeletionTask(@PathVariable Long taskId,
                                             @RequestParam(value = "executionMode", required = false) String executionMode) {
        return RagApiResponse.ok(adminTenantService.runDeletionTask(taskId, executionMode));
    }

    @PostMapping("/tenant-deletion-tasks/{taskId}/cancel")
    @Operation(summary = "Cancel tenant data deletion task")
    public RagApiResponse<?> cancelDeletionTask(@PathVariable Long taskId) {
        return RagApiResponse.ok(adminTenantService.cancelDeletionTask(taskId));
    }

    @PostMapping("/tenant-deletion-tasks/{taskId}/stages/{stageCode}/retry")
    @Operation(summary = "Retry one failed tenant deletion stage")
    public RagApiResponse<?> retryDeletionStage(@PathVariable Long taskId,
                                                @PathVariable String stageCode,
                                                @RequestParam(value = "executionMode", required = false) String executionMode) {
        return RagApiResponse.ok(adminTenantService.retryDeletionStage(taskId, stageCode, executionMode));
    }
}

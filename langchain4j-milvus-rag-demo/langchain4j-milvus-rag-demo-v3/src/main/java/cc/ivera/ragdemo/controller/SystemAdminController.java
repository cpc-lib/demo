package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.admin.SystemTenantRequest;
import cc.ivera.ragdemo.admin.SystemTenantUserAdminService;
import cc.ivera.ragdemo.admin.SystemUserRequest;
import cc.ivera.ragdemo.admin.UserPasswordResetRequest;
import cc.ivera.ragdemo.admin.UserRolesUpdateRequest;
import cc.ivera.ragdemo.domain.tenant.SysRole;
import cc.ivera.ragdemo.domain.tenant.SysTenant;
import cc.ivera.ragdemo.domain.tenant.SysUser;
import cc.ivera.ragdemo.domain.tenant.SysUserRole;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "System Admin", description = "Platform tenant and user management")
public class SystemAdminController {

    private final SystemTenantUserAdminService service;

    @GetMapping("/tenants")
    @Operation(summary = "List tenants")
    public RagApiResponse<List<SysTenant>> listTenants(@RequestParam(value = "keyword", required = false) String keyword,
                                                       @RequestParam(value = "status", required = false) Integer status,
                                                       @RequestParam(value = "limit", required = false) Integer limit) {
        return RagApiResponse.ok(service.listTenants(keyword, status, limit));
    }

    @PostMapping("/tenants")
    @Operation(summary = "Create tenant")
    public RagApiResponse<SysTenant> createTenant(@RequestBody SystemTenantRequest request) {
        return RagApiResponse.ok(service.createTenant(request));
    }

    @PutMapping("/tenants/{tenantId}")
    @Operation(summary = "Update tenant")
    public RagApiResponse<SysTenant> updateTenant(@PathVariable Long tenantId,
                                                  @RequestBody SystemTenantRequest request) {
        return RagApiResponse.ok(service.updateTenant(tenantId, request));
    }

    @PutMapping("/tenants/{tenantId}/enable")
    @Operation(summary = "Enable tenant")
    public RagApiResponse<SysTenant> enableTenant(@PathVariable Long tenantId) {
        return RagApiResponse.ok(service.enableTenant(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/disable")
    @Operation(summary = "Disable tenant")
    public RagApiResponse<SysTenant> disableTenant(@PathVariable Long tenantId) {
        return RagApiResponse.ok(service.disableTenant(tenantId));
    }

    @GetMapping("/users")
    @Operation(summary = "List users")
    public RagApiResponse<List<SysUser>> listUsers(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        return RagApiResponse.ok(service.listUsers(tenantId, keyword, status, limit));
    }

    @PostMapping("/users")
    @Operation(summary = "Create user")
    public RagApiResponse<SysUser> createUser(@RequestBody SystemUserRequest request) {
        return RagApiResponse.ok(service.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user")
    public RagApiResponse<SysUser> updateUser(@PathVariable Long id,
                                              @RequestBody SystemUserRequest request) {
        return RagApiResponse.ok(service.updateUser(id, request));
    }

    @PutMapping("/users/{id}/enable")
    @Operation(summary = "Enable user")
    public RagApiResponse<SysUser> enableUser(@PathVariable Long id) {
        return RagApiResponse.ok(service.enableUser(id));
    }

    @PutMapping("/users/{id}/disable")
    @Operation(summary = "Disable user")
    public RagApiResponse<SysUser> disableUser(@PathVariable Long id) {
        return RagApiResponse.ok(service.disableUser(id));
    }

    @PutMapping("/users/{id}/reset-password")
    @Operation(summary = "Reset user password")
    public RagApiResponse<SysUser> resetUserPassword(@PathVariable Long id,
                                                     @RequestBody UserPasswordResetRequest request) {
        return RagApiResponse.ok(service.resetUserPassword(id, request));
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles")
    public RagApiResponse<List<SysRole>> listRoles(@RequestParam(value = "tenantId", required = false) Long tenantId) {
        return RagApiResponse.ok(service.listRoles(tenantId));
    }

    @GetMapping("/users/{id}/roles")
    @Operation(summary = "List user roles")
    public RagApiResponse<List<SysUserRole>> listUserRoles(@PathVariable Long id) {
        return RagApiResponse.ok(service.listUserRoles(id));
    }

    @PutMapping("/users/{id}/roles")
    @Operation(summary = "Replace user roles")
    public RagApiResponse<List<SysUserRole>> updateUserRoles(@PathVariable Long id,
                                                             @RequestBody UserRolesUpdateRequest request) {
        return RagApiResponse.ok(service.updateUserRoles(id, request));
    }
}

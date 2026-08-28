package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.admin.SystemUserRequest;
import cc.ivera.ragdemo.admin.TenantUserAdminService;
import cc.ivera.ragdemo.admin.UserPasswordResetRequest;
import cc.ivera.ragdemo.admin.UserRolesUpdateRequest;
import cc.ivera.ragdemo.domain.tenant.SysRole;
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
@RequestMapping("/api/admin/tenant")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "Tenant User Admin", description = "Current tenant user and role management")
public class TenantUserAdminController {

    private final TenantUserAdminService service;

    @GetMapping("/users")
    @Operation(summary = "List current tenant users")
    public RagApiResponse<List<SysUser>> listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                                   @RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        return RagApiResponse.ok(service.listUsers(keyword, status, limit));
    }

    @PostMapping("/users")
    @Operation(summary = "Create current tenant user")
    public RagApiResponse<SysUser> createUser(@RequestBody SystemUserRequest request) {
        return RagApiResponse.ok(service.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update current tenant user")
    public RagApiResponse<SysUser> updateUser(@PathVariable Long id,
                                              @RequestBody SystemUserRequest request) {
        return RagApiResponse.ok(service.updateUser(id, request));
    }

    @PutMapping("/users/{id}/enable")
    @Operation(summary = "Enable current tenant user")
    public RagApiResponse<SysUser> enableUser(@PathVariable Long id) {
        return RagApiResponse.ok(service.enableUser(id));
    }

    @PutMapping("/users/{id}/disable")
    @Operation(summary = "Disable current tenant user")
    public RagApiResponse<SysUser> disableUser(@PathVariable Long id) {
        return RagApiResponse.ok(service.disableUser(id));
    }

    @PutMapping("/users/{id}/reset-password")
    @Operation(summary = "Reset current tenant user password")
    public RagApiResponse<SysUser> resetUserPassword(@PathVariable Long id,
                                                     @RequestBody UserPasswordResetRequest request) {
        return RagApiResponse.ok(service.resetUserPassword(id, request));
    }

    @GetMapping("/roles")
    @Operation(summary = "List current tenant roles")
    public RagApiResponse<List<SysRole>> listRoles() {
        return RagApiResponse.ok(service.listRoles());
    }

    @GetMapping("/users/{id}/roles")
    @Operation(summary = "List current tenant user roles")
    public RagApiResponse<List<SysUserRole>> listUserRoles(@PathVariable Long id) {
        return RagApiResponse.ok(service.listUserRoles(id));
    }

    @PutMapping("/users/{id}/roles")
    @Operation(summary = "Replace current tenant user roles")
    public RagApiResponse<List<SysUserRole>> updateUserRoles(@PathVariable Long id,
                                                             @RequestBody UserRolesUpdateRequest request) {
        return RagApiResponse.ok(service.updateUserRoles(id, request));
    }
}

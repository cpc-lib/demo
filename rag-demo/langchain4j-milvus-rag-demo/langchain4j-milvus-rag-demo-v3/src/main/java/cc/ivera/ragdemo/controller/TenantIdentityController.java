package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.tenant.TenantIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "Tenant Identity", description = "Current authenticated tenant and user context")
public class TenantIdentityController {

    private final TenantIdentityService service;

    @GetMapping
    @Operation(summary = "Get current tenant user context")
    public RagApiResponse<?> me() {
        return RagApiResponse.ok(service.currentUser());
    }

    @GetMapping("/tenants")
    @Operation(summary = "List tenants visible to the current user")
    public RagApiResponse<?> tenants() {
        return RagApiResponse.ok(service.currentTenants());
    }

    @GetMapping("/knowledge-bases")
    @Operation(summary = "List knowledge bases visible to the current user")
    public RagApiResponse<?> knowledgeBases() {
        return RagApiResponse.ok(service.currentKnowledgeBases());
    }
}

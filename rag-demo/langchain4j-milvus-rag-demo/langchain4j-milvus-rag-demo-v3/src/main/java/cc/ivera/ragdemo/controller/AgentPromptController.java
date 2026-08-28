package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.model.dto.EntityDtoConverter;
import cc.ivera.ragdemo.model.dto.RagAgentPromptDto;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.agent.PromptService;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/admin/agent-prompts")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AgentPromptController {

    private final PromptService promptService;
    private final EntityDtoConverter converter;

    @GetMapping
    public RagApiResponse<RagAgentPromptDto> getActivePrompt() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(promptService.getActivePrompt(tenantId)));
    }

    @GetMapping("/all")
    public RagApiResponse<List<RagAgentPromptDto>> listPrompts() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toAgentPromptDtoList(promptService.listPrompts(tenantId)));
    }

    @PostMapping
    public RagApiResponse<RagAgentPromptDto> createPrompt(@RequestBody AgentPromptRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        String updatedBy = currentOperator();
        return RagApiResponse.ok(converter.toDto(promptService.createPrompt(
                tenantId,
                request.promptName(),
                requirePromptContent(request),
                request.enabled(),
                updatedBy
        )));
    }

    @PutMapping
    public RagApiResponse<RagAgentPromptDto> updatePrompt(@RequestBody AgentPromptRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(promptService.updatePrompt(
                tenantId,
                request.promptName(),
                requirePromptContent(request),
                currentOperator()
        )));
    }

    @PutMapping("/{id}")
    public RagApiResponse<RagAgentPromptDto> updatePromptById(
            @PathVariable Long id,
            @RequestBody AgentPromptRequest request
    ) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(promptService.updatePromptById(
                tenantId,
                id,
                request.promptName(),
                requirePromptContent(request),
                request.enabled(),
                currentOperator()
        )));
    }

    @PutMapping("/{id}/enable")
    public RagApiResponse<RagAgentPromptDto> enablePrompt(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(promptService.enablePrompt(tenantId, id, currentOperator())));
    }

    @PutMapping("/{id}/disable")
    public RagApiResponse<RagAgentPromptDto> disablePrompt(@PathVariable Long id) {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toDto(promptService.disablePrompt(tenantId, id, currentOperator())));
    }

    @GetMapping("/versions")
    public RagApiResponse<List<RagAgentPromptDto>> listVersions() {
        Long tenantId = TenantContextHolder.requireTenantId();
        return RagApiResponse.ok(converter.toAgentPromptDtoList(promptService.listVersions(tenantId)));
    }

    @PostMapping("/rollback")
    public RagApiResponse<RagAgentPromptDto> rollback(@RequestBody AgentPromptRollbackRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        Integer version = request.version();
        if (version == null) {
            throw new IllegalArgumentException("version is required");
        }
        return RagApiResponse.ok(converter.toDto(promptService.rollbackToVersion(tenantId, version, currentOperator())));
    }

    private String requirePromptContent(AgentPromptRequest request) {
        String content = request == null ? null : request.promptContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("promptContent is required");
        }
        return content;
    }

    private String currentOperator() {
        return TenantContextHolder.current().map(ctx -> ctx.operatorUserId()).orElse("admin");
    }

    public record AgentPromptRequest(String promptName, String promptContent, Boolean enabled) {
    }

    public record AgentPromptRollbackRequest(Integer version) {
    }
}

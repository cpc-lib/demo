package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.model.dto.EntityDtoConverter;
import cc.ivera.ragdemo.model.dto.RagKnowledgeBaseDto;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.permission.KnowledgeBaseMemberRequest;
import cc.ivera.ragdemo.permission.KnowledgeBasePermissionService;
import cc.ivera.ragdemo.service.RagKnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag/knowledge-bases")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG 知识库", description = "知识库创建、查询和分页列表接口")
public class RagKnowledgeBaseController {

    private final RagKnowledgeBaseService service;
    private final KnowledgeBasePermissionService permissionService;
    private final EntityDtoConverter converter;

    @GetMapping
    @Operation(summary = "分页查询知识库", description = "按租户过滤知识库，支持分页和数据库侧排序。")
    public RagApiResponse<PageResponse<RagKnowledgeBaseDto>> list(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                               @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                               @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                               @RequestParam(value = "limit", required = false) Integer limit,
                                                               @RequestParam(value = "sortBy", required = false) String sortBy,
                                                               @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<RagKnowledgeBase> page = service.page(
                tenantId,
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500)
        );
        return RagApiResponse.ok(new PageResponse<>(
                page.pageNo(),
                page.pageSize(),
                page.total(),
                page.pages(),
                page.maxPageSize(),
                page.sortBy(),
                page.sortDirection(),
                converter.toKnowledgeBaseDtoList(page.records())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "按知识库 ID 查询知识库配置。")
    public RagApiResponse<RagKnowledgeBaseDto> get(@PathVariable Long id) {
        return RagApiResponse.ok(converter.toDto(service.getRequired(id)));
    }

    @PostMapping
    @Operation(summary = "创建知识库", description = "创建知识库；租户和编码重复时返回已有记录。")
    public RagApiResponse<RagKnowledgeBaseDto> create(@Valid @RequestBody RagKnowledgeBaseCreateRequest request) {
        return RagApiResponse.ok(converter.toDto(service.create(request)));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List knowledge base members", description = "Require ADMIN or above for the current tenant.")
    public RagApiResponse<?> listMembers(@PathVariable Long id) {
        return RagApiResponse.ok(permissionService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Create or update a knowledge base member", description = "Require OWNER for the current tenant.")
    public RagApiResponse<?> upsertMember(@PathVariable Long id,
                                          @RequestBody KnowledgeBaseMemberRequest request) {
        return RagApiResponse.ok(permissionService.upsertMember(id, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a knowledge base member", description = "Require OWNER for the current tenant.")
    public RagApiResponse<?> removeMember(@PathVariable Long id,
                                          @PathVariable String userId) {
        permissionService.removeMember(id, userId);
        return RagApiResponse.ok(java.util.Map.of("removed", true));
    }
}

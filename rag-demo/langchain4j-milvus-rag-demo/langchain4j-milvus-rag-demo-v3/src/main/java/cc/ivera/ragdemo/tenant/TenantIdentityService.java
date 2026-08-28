package cc.ivera.ragdemo.tenant;


import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.domain.tenant.SysTenant;
import cc.ivera.ragdemo.mapper.SysTenantMapper;
import cc.ivera.ragdemo.service.RagKnowledgeBaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantIdentityService {

    private final SysTenantMapper tenantMapper;
    private final RagKnowledgeBaseService knowledgeBaseService;

    public CurrentUserResponse currentUser() {
        TenantContext context = TenantContextHolder.require();
        UserContext user = context.user();
        return new CurrentUserResponse(
                context.tenantId(),
                context.operatorTenantId(),
                context.tenantExternalId(),
                user.userId(),
                user.displayName(),
                user.roles(),
                user.authorizedKnowledgeBaseIds(),
                user.permissionTags(),
                user.platformAdmin(),
                context.impersonating(),
                context.requestId(),
                context.sourceIp()
        );
    }

    public List<SysTenant> currentTenants() {
        TenantContext context = TenantContextHolder.require();
        if (context.platformAdmin()) {
            return tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                    .eq(SysTenant::getIsDeleted, 0)
                    .orderByDesc(SysTenant::getCreatedAt)
                    .last("LIMIT 500"));
        }
        return tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getId, context.tenantId())
                .eq(SysTenant::getIsDeleted, 0));
    }

    public List<RagKnowledgeBase> currentKnowledgeBases() {
        TenantContext context = TenantContextHolder.require();
        if (context.tenantId() == null) {
            return List.of();
        }
        List<RagKnowledgeBase> bases = knowledgeBaseService.list(context.tenantId());
        List<Long> scopedKbIds = context.user().authorizedKnowledgeBaseIds();
        if (!context.platformAdmin() && !context.user().hasRole("TENANT_ADMIN") && !scopedKbIds.isEmpty()) {
            return bases.stream()
                    .filter(kb -> scopedKbIds.contains(kb.getId()))
                    .toList();
        }
        return bases;
    }
}

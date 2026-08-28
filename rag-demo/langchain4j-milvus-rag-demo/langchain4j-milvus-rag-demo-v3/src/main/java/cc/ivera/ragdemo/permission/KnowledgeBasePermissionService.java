package cc.ivera.ragdemo.permission;


import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.domain.tenant.RagKnowledgeBaseMember;
import cc.ivera.ragdemo.exception.TenantAccessDeniedException;
import cc.ivera.ragdemo.mapper.RagKnowledgeBaseMapper;
import cc.ivera.ragdemo.mapper.RagKnowledgeBaseMemberMapper;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KnowledgeBasePermissionService {

    private final RagKnowledgeBaseMapper knowledgeBaseMapper;
    private final RagKnowledgeBaseMemberMapper memberMapper;

    public void requireRead(RagKnowledgeBase knowledgeBase) {
        requireRole(knowledgeBase, KnowledgeBaseRole.READER);
    }

    public void requireEditor(RagKnowledgeBase knowledgeBase) {
        requireRole(knowledgeBase, KnowledgeBaseRole.EDITOR);
    }

    public void requireAdmin(RagKnowledgeBase knowledgeBase) {
        requireRole(knowledgeBase, KnowledgeBaseRole.ADMIN);
    }

    public void requireOwner(RagKnowledgeBase knowledgeBase) {
        requireRole(knowledgeBase, KnowledgeBaseRole.OWNER);
    }

    public void requireRole(RagKnowledgeBase knowledgeBase, KnowledgeBaseRole requiredRole) {
        if (knowledgeBase == null) {
            throw new TenantAccessDeniedException("Knowledge base is required for permission check");
        }
        TenantContext context = TenantContextHolder.require();
        Long tenantId = context.tenantId();
        if (!tenantId.equals(knowledgeBase.getTenantId())) {
            throw new TenantAccessDeniedException("Knowledge base belongs to another tenant");
        }
        UserContext user = context.user();
        if (context.systemContext() || user.platformAdmin() || user.hasRole("TENANT_ADMIN") || user.hasRole("KB_OWNER")) {
            return;
        }
        KnowledgeBaseRole actual = memberRole(tenantId, knowledgeBase.getId(), user.userId());
        if (actual == null || !actual.atLeast(requiredRole)) {
            throw new TenantAccessDeniedException("User is not authorized for knowledge base " + knowledgeBase.getId());
        }
    }

    public List<Long> authorizedKnowledgeBaseIds(List<Long> requestedIds, KnowledgeBaseRole requiredRole) {
        TenantContext context = TenantContextHolder.require();
        List<Long> cleaned = requestedIds == null ? List.of() : requestedIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            return List.of();
        }
        if (context.systemContext() || context.platformAdmin()
                || context.user().hasRole("TENANT_ADMIN") || context.user().hasRole("KB_OWNER")) {
            return ownedKnowledgeBaseIds(context.tenantId(), cleaned);
        }
        Set<Long> headerScope = new LinkedHashSet<>(context.user().authorizedKnowledgeBaseIds());
        List<Long> ids = cleaned.stream()
                .filter(id -> headerScope.isEmpty() || headerScope.contains(id))
                .filter(id -> {
                    RagKnowledgeBase kb = knowledgeBaseMapper.selectById(id);
                    return kb != null
                            && context.tenantId().equals(kb.getTenantId())
                            && !Integer.valueOf(1).equals(kb.getIsDeleted())
                            && hasRole(context.tenantId(), id, context.user().userId(), requiredRole);
                })
                .toList();
        if (ids.isEmpty()) {
            throw new TenantAccessDeniedException("No authorized knowledge base in request");
        }
        return ids;
    }

    public List<RagKnowledgeBaseMember> listMembers(Long knowledgeBaseId) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagKnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        requireAdmin(kb);
        return memberMapper.selectList(new LambdaQueryWrapper<RagKnowledgeBaseMember>()
                .eq(RagKnowledgeBaseMember::getTenantId, tenantId)
                .eq(RagKnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(RagKnowledgeBaseMember::getIsDeleted, 0)
                .orderByDesc(RagKnowledgeBaseMember::getUpdatedAt));
    }

    public RagKnowledgeBaseMember upsertMember(Long knowledgeBaseId, KnowledgeBaseMemberRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagKnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        requireOwner(kb);
        if (request == null || !StringUtils.hasText(request.userId())) {
            throw new IllegalArgumentException("userId is required");
        }
        KnowledgeBaseRole role = KnowledgeBaseRole.from(request.role());
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        RagKnowledgeBaseMember existing = memberMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBaseMember>()
                .eq(RagKnowledgeBaseMember::getTenantId, tenantId)
                .eq(RagKnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(RagKnowledgeBaseMember::getUserId, request.userId().trim())
                .last("LIMIT 1"));
        RagKnowledgeBaseMember member = existing == null ? new RagKnowledgeBaseMember() : existing;
        member.setTenantId(tenantId);
        member.setKnowledgeBaseId(knowledgeBaseId);
        member.setUserId(request.userId().trim());
        member.setMemberRole(role.name());
        member.setPermissionTags(String.join(",", request.permissionTags() == null ? List.of() : request.permissionTags()));
        member.setIsDeleted(0);
        member.setUpdatedAt(LocalDateTime.now());
        if (existing == null) {
            member.setCreatedAt(LocalDateTime.now());
            memberMapper.insert(member);
        } else {
            memberMapper.updateById(member);
        }
        return member;
    }

    public void removeMember(Long knowledgeBaseId, String userId) {
        Long tenantId = TenantContextHolder.requireTenantId();
        RagKnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        requireOwner(kb);
        memberMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RagKnowledgeBaseMember>()
                .eq(RagKnowledgeBaseMember::getTenantId, tenantId)
                .eq(RagKnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(RagKnowledgeBaseMember::getUserId, userId)
                .set(RagKnowledgeBaseMember::getIsDeleted, 1)
                .set(RagKnowledgeBaseMember::getUpdatedAt, LocalDateTime.now()));
    }

    private List<Long> ownedKnowledgeBaseIds(Long tenantId, List<Long> requestedIds) {
        List<RagKnowledgeBase> rows = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getTenantId, tenantId)
                .in(RagKnowledgeBase::getId, requestedIds)
                .eq(RagKnowledgeBase::getIsDeleted, 0));
        List<Long> ids = rows.stream().map(RagKnowledgeBase::getId).toList();
        if (ids.isEmpty()) {
            throw new TenantAccessDeniedException("No knowledge base belongs to current tenant");
        }
        return ids;
    }

    private boolean hasRole(Long tenantId, Long knowledgeBaseId, String userId, KnowledgeBaseRole requiredRole) {
        KnowledgeBaseRole role = memberRole(tenantId, knowledgeBaseId, userId);
        return role != null && role.atLeast(requiredRole);
    }

    private KnowledgeBaseRole memberRole(Long tenantId, Long knowledgeBaseId, String userId) {
        RagKnowledgeBaseMember member = memberMapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBaseMember>()
                .eq(RagKnowledgeBaseMember::getTenantId, tenantId)
                .eq(RagKnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
                .eq(RagKnowledgeBaseMember::getUserId, userId)
                .eq(RagKnowledgeBaseMember::getIsDeleted, 0)
                .last("LIMIT 1"));
        return member == null ? null : KnowledgeBaseRole.from(member.getMemberRole());
    }
}

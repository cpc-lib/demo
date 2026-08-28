package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.controller.RagKnowledgeBaseCreateRequest;
import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.mapper.RagKnowledgeBaseMapper;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.permission.KnowledgeBasePermissionService;
import cc.ivera.ragdemo.permission.KnowledgeBaseRole;
import cc.ivera.ragdemo.service.vector.TenantMilvusCollectionResolver;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagKnowledgeBaseService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;

    private final RagKnowledgeBaseMapper mapper;
    private final RagProperties properties;
    private final TenantMilvusCollectionResolver collectionResolver;
    private KnowledgeBasePermissionService permissionService;

    @Autowired(required = false)
    public void setPermissionService(KnowledgeBasePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public RagKnowledgeBase create(RagKnowledgeBaseCreateRequest request) {
        Long tenantId = effectiveTenantId(request.tenantId());
        RagKnowledgeBase existing = mapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getTenantId, tenantId)
                .eq(RagKnowledgeBase::getKbCode, request.kbCode())
                .eq(RagKnowledgeBase::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        RagKnowledgeBase kb = defaultKnowledgeBase(tenantId, request.kbCode(), request.name(), request.description());
        mapper.insert(kb);
        return kb;
    }

    public RagKnowledgeBase getOrCreateDefault() {
        Long tenantId = effectiveTenantId(properties.getIngestion().getDefaultTenantId());
        return ensureDefaultExists(tenantId);
    }

    private RagKnowledgeBase ensureDefaultExists(Long tenantId) {
        String kbCode = properties.getIngestion().getDefaultKnowledgeBaseCode();
        RagKnowledgeBase existing = mapper.selectOne(new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getTenantId, tenantId)
                .eq(RagKnowledgeBase::getKbCode, kbCode)
                .eq(RagKnowledgeBase::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        RagKnowledgeBase kb = defaultKnowledgeBase(
                tenantId,
                kbCode,
                properties.getIngestion().getDefaultKnowledgeBaseName(),
                "Default RAG knowledge base"
        );
        mapper.insert(kb);
        return kb;
    }

    public RagKnowledgeBase getRequired(Long id) {
        RagKnowledgeBase kb = mapper.selectById(id);
        if (kb == null || Integer.valueOf(1).equals(kb.getIsDeleted())) {
            throw new IllegalArgumentException("Knowledge base not found: " + id);
        }
        assertTenantMatches(kb);
        if (permissionService != null) {
            permissionService.requireRead(kb);
        }
        return kb;
    }

    public List<RagKnowledgeBase> list(Long tenantId) {
        Long effectiveTenantId = effectiveTenantId(tenantId);
        ensureDefaultExists(effectiveTenantId);
        return mapper.selectList(new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getTenantId, effectiveTenantId)
                .eq(RagKnowledgeBase::getIsDeleted, 0)
                .orderByDesc(RagKnowledgeBase::getCreatedAt));
    }

    public PageResponse<RagKnowledgeBase> page(Long tenantId, PageQuery pageQuery) {
        Long effectiveTenantId = effectiveTenantId(tenantId);
        ensureDefaultExists(effectiveTenantId);
        PageQuery query = normalizePageQuery(pageQuery);
        long total = mapper.selectCount(queryWrapper(effectiveTenantId));
        LambdaQueryWrapper<RagKnowledgeBase> rowsQuery = queryWrapper(effectiveTenantId);
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, mapper.selectList(rowsQuery));
    }

    private LambdaQueryWrapper<RagKnowledgeBase> queryWrapper(Long tenantId) {
        return new LambdaQueryWrapper<RagKnowledgeBase>()
                .eq(RagKnowledgeBase::getTenantId, tenantId)
                .eq(RagKnowledgeBase::getIsDeleted, 0);
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery) {
        PageQuery query = pageQuery == null
                ? PageQuery.of(1, null, DEFAULT_PAGE_SIZE, "createdAt", "DESC", MAX_PAGE_SIZE)
                : pageQuery;
        return query.withDefaultSort("createdAt", "DESC");
    }

    private void applyOrder(LambdaQueryWrapper<RagKnowledgeBase> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getId);
            case "createdAt" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getUpdatedAt);
            case "name" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getName);
            case "kbCode" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getKbCode);
            case "status" -> wrapper.orderBy(true, asc, RagKnowledgeBase::getStatus);
            default -> wrapper.orderByDesc(RagKnowledgeBase::getCreatedAt);
        }
    }

    private RagKnowledgeBase defaultKnowledgeBase(Long tenantId, String kbCode, String name, String description) {
        RagKnowledgeBase kb = new RagKnowledgeBase();
        kb.setTenantId(tenantId == null ? 0L : tenantId);
        kb.setKbCode(kbCode);
        kb.setName(name);
        kb.setDescription(description);
        kb.setVectorStoreType("milvus");
        kb.setVectorCollection(collectionResolver.collectionForTenant(properties.getMilvus().getCollection(), tenantId));
        kb.setEmbeddingModel(properties.getEmbedding().getModel());
        kb.setEmbeddingDimension(properties.getEmbedding().getDimension());
        kb.setChunkStrategy("recursive");
        kb.setChunkSize(properties.getSplitter().getChunkSize());
        kb.setChunkOverlap(properties.getSplitter().getOverlap());
        kb.setRetrievalTopK(properties.getMilvus().getTopK());
        kb.setMinScore(BigDecimal.valueOf(properties.getMilvus().getMinScore()));
        kb.setConfigJson("{}");
        kb.setStatus(1);
        kb.setLockVersion(0L);
        kb.setIsDeleted(0);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());
        return kb;
    }

    public List<Long> authorizedKnowledgeBaseIds(List<Long> requestedIds, KnowledgeBaseRole requiredRole) {
        if (permissionService == null) {
            return requestedIds == null ? List.of() : requestedIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        }
        return permissionService.authorizedKnowledgeBaseIds(requestedIds, requiredRole);
    }

    private Long effectiveTenantId(Long requestedTenantId) {
        return TenantContextHolder.currentTenantId()
                .orElse(requestedTenantId == null ? properties.getIngestion().getDefaultTenantId() : requestedTenantId);
    }

    private void assertTenantMatches(RagKnowledgeBase kb) {
        TenantContextHolder.currentTenantId().ifPresent(currentTenant -> {
            if (!currentTenant.equals(kb.getTenantId())) {
                throw new cc.ivera.ragdemo.exception.TenantAccessDeniedException("Knowledge base belongs to another tenant");
            }
        });
    }
}

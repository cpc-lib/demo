package cc.ivera.ragdemo.tenant;


import cc.ivera.ragdemo.model.query.RagQueryRequest;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.model.query.RagSearchRequest;
import cc.ivera.ragdemo.permission.KnowledgeBasePermissionService;
import cc.ivera.ragdemo.permission.KnowledgeBaseRole;
import cc.ivera.ragdemo.service.ragops.CrossTenantGuardPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantScopedQueryService {

    private final KnowledgeBasePermissionService permissionService;
    private final CrossTenantGuardPolicy guardPolicy = new CrossTenantGuardPolicy();

    public RagRetrievalCriteria criteria(RagQueryRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        List<Long> knowledgeBaseIds = permissionService.authorizedKnowledgeBaseIds(request.knowledgeBaseIds(), KnowledgeBaseRole.READER);
        return new RagRetrievalCriteria(
                request.question(),
                request.imageUrl(),
                request.imageAssetId(),
                request.imageBase64(),
                request.modalities(),
                tenantId,
                knowledgeBaseIds,
                request.retrievalMode(),
                request.topK(),
                request.minScore(),
                request.textVectorWeight(),
                request.imageVectorWeight(),
                request.keywordWeight(),
                request.includeReviewPending(),
                request.contentTypes(),
                request.permissionTags()
        );
    }

    public RagRetrievalCriteria criteria(RagSearchRequest request) {
        Long tenantId = TenantContextHolder.requireTenantId();
        List<Long> knowledgeBaseIds = permissionService.authorizedKnowledgeBaseIds(request.knowledgeBaseIds(), KnowledgeBaseRole.READER);
        return new RagRetrievalCriteria(
                request.query(),
                request.imageUrl(),
                request.imageAssetId(),
                request.imageBase64(),
                request.modalities(),
                tenantId,
                knowledgeBaseIds,
                request.retrievalMode(),
                request.topK(),
                request.minScore(),
                request.textVectorWeight(),
                request.imageVectorWeight(),
                request.keywordWeight(),
                request.includeReviewPending(),
                request.contentTypes(),
                request.permissionTags()
        );
    }

    public List<RagSearchItem> filterAuthorized(List<RagSearchItem> items, RagRetrievalCriteria criteria) {
        return guardPolicy.filterAllowed(items, criteria.tenantId(), criteria.knowledgeBaseIds(), criteria.permissionTags());
    }
}

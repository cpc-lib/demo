package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;
import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskRetryResponse;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.ragops.IngestionTaskGovernancePolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagIngestionTaskGovernanceService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final RagIngestionTaskMapper taskMapper;
    private final RagIngestionTaskPublisher taskPublisher;
    private final IngestionTaskCancellationService cancellationService;

    public RagIngestionTask getRequired(Long taskId) {
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Ingestion task not found: " + taskId);
        }
        TenantContextHolder.currentTenantId().ifPresent(currentTenant -> {
            if (!currentTenant.equals(task.getTenantId())) {
                throw new cc.ivera.ragdemo.exception.TenantAccessDeniedException("Ingestion task belongs to another tenant");
            }
        });
        return task;
    }

    public List<RagIngestionTask> listTasks(Long tenantId,
                                            Long knowledgeBaseId,
                                            Long documentId,
                                            Integer status,
                                            String taskType,
                                            Integer limit) {
        return pageTasks(
                tenantId,
                knowledgeBaseId,
                documentId,
                status,
                taskType,
                PageQuery.of(1, limit, DEFAULT_LIMIT, "createdAt", "DESC", MAX_LIMIT)
        ).records();
    }

    public PageResponse<RagIngestionTask> pageTasks(Long tenantId,
                                                    Long knowledgeBaseId,
                                                    Long documentId,
                                                    Integer status,
                                                    String taskType,
                                                    PageQuery pageQuery) {
        validateStatus(status);
        PageQuery query = normalizePageQuery(pageQuery);
        Long effectiveTenantId = effectiveTenantId(tenantId);
        long total = taskMapper.selectCount(queryWrapper(effectiveTenantId, knowledgeBaseId, documentId, status, taskType));
        LambdaQueryWrapper<RagIngestionTask> rowsQuery = queryWrapper(effectiveTenantId, knowledgeBaseId, documentId, status, taskType);
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, taskMapper.selectList(rowsQuery));
    }

    @Transactional
    public RagIngestionTask cancel(Long taskId) {
        return cancellationService.requestCancel(taskId, "api");
    }

    @Transactional
    public RagIngestionTaskRetryResponse retry(Long taskId) {
        RagIngestionTask task = getRequired(taskId);
        IngestionTaskStatus status = IngestionTaskStatus.fromCode(task.getTaskStatus());
        IngestionTaskGovernancePolicy.assertCanRetry(status, task.getRetryCount(), task.getMaxRetryCount());

        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus(IngestionTaskStatus.RETRY_WAIT.code());
        task.setRetryCount(IngestionTaskGovernancePolicy.nextRetryCount(task.getRetryCount()));
        task.setProgress(0);
        task.setStageProgress(0);
        task.setCurrentStage(null);
        task.setNextRetryAt(now);
        task.setCancelRequested(false);
        task.setCancelRequestedAt(null);
        task.setCancelRequestedBy(null);
        task.setPartialSuccess(false);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);

        boolean published = taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(
                task.getTenantId(),
                task.getId(),
                task.getDocumentId(),
                task.getKnowledgeBaseId(),
                task.getDocumentVersionId(),
                task.getTaskNo()
        ));
        return new RagIngestionTaskRetryResponse(task, published);
    }

    private void validateStatus(Integer status) {
        if (status != null) {
            IngestionTaskStatus.fromCode(status);
        }
    }

    private LambdaQueryWrapper<RagIngestionTask> queryWrapper(Long tenantId,
                                                             Long knowledgeBaseId,
                                                             Long documentId,
                                                             Integer status,
                                                             String taskType) {
        return new LambdaQueryWrapper<RagIngestionTask>()
                .eq(tenantId != null, RagIngestionTask::getTenantId, tenantId)
                .eq(knowledgeBaseId != null, RagIngestionTask::getKnowledgeBaseId, knowledgeBaseId)
                .eq(documentId != null, RagIngestionTask::getDocumentId, documentId)
                .eq(status != null, RagIngestionTask::getTaskStatus, status)
                .eq(StringUtils.hasText(taskType), RagIngestionTask::getTaskType, taskType);
    }

    private Long effectiveTenantId(Long requestTenantId) {
        return TenantContextHolder.currentTenantId().orElse(requestTenantId);
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery) {
        if (pageQuery == null) {
            return PageQuery.of(1, null, DEFAULT_LIMIT, "createdAt", "DESC", MAX_LIMIT);
        }
        PageQuery query = PageQuery.of(
                pageQuery.pageNo(),
                pageQuery.pageSize(),
                pageQuery.pageSize(),
                pageQuery.sortBy(),
                pageQuery.sortDirection(),
                MAX_LIMIT
        );
        return query.withDefaultSort("createdAt", "DESC");
    }

    private void applyOrder(LambdaQueryWrapper<RagIngestionTask> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagIngestionTask::getId);
            case "createdAt" -> wrapper.orderBy(true, asc, RagIngestionTask::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagIngestionTask::getUpdatedAt);
            case "progress" -> wrapper.orderBy(true, asc, RagIngestionTask::getProgress);
            case "taskStatus", "status" -> wrapper.orderBy(true, asc, RagIngestionTask::getTaskStatus);
            case "retryCount" -> wrapper.orderBy(true, asc, RagIngestionTask::getRetryCount);
            default -> wrapper.orderByDesc(RagIngestionTask::getCreatedAt);
        }
    }
}

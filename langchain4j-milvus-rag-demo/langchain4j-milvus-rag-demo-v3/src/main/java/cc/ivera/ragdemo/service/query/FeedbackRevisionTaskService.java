package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.domain.rag.RagFeedbackRevisionTask;
import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.domain.rag.RagQueryFeedbackEvent;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.mapper.RagFeedbackRevisionTaskMapper;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackEventMapper;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackMapper;
import cc.ivera.ragdemo.mapper.RagQueryLogMapper;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagFeedbackRevisionTaskActionRequest;
import cc.ivera.ragdemo.model.query.RagFeedbackRevisionTaskRequest;
import cc.ivera.ragdemo.service.ragops.FeedbackRevisionPolicy;
import cc.ivera.ragdemo.service.ragops.QueryFeedbackWorkflowPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class FeedbackRevisionTaskService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter REVISION_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RagFeedbackRevisionTaskMapper taskMapper;
    private final RagQueryFeedbackMapper feedbackMapper;
    private final RagQueryFeedbackEventMapper eventMapper;
    private final RagQueryLogMapper queryLogMapper;
    private final FeedbackRevisionPolicy revisionPolicy;
    private final QueryFeedbackWorkflowPolicy workflowPolicy;
    private final QueryFeedbackWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RagFeedbackRevisionTask createFromFeedback(Long feedbackId, RagFeedbackRevisionTaskRequest request) {
        RagQueryFeedback feedback = requiredFeedback(feedbackId);
        RagQueryLog log = feedback.getQueryLogId() == null ? null : queryLogMapper.selectById(feedback.getQueryLogId());
        RagFeedbackRevisionTask task = new RagFeedbackRevisionTask();
        task.setRevisionNo(newRevisionNo());
        task.setFeedbackId(feedback.getId());
        task.setQueryLogId(feedback.getQueryLogId());
        task.setTenantId(log == null ? null : log.getTenantId());
        task.setKnowledgeBaseId(request == null ? null : request.knowledgeBaseId());
        task.setDocumentId(request == null ? null : request.documentId());
        task.setChunkUid(revisionPolicy.cleanText(request == null ? null : request.chunkUid(), 128));
        task.setRevisionType(revisionPolicy.normalizeType(request == null ? null : request.revisionType()));
        task.setRevisionStatus(FeedbackRevisionPolicy.STATUS_PLANNED);
        task.setBeforeSnapshotJson(revisionPolicy.cleanText(request == null ? null : request.beforeSnapshotJson(), 20000));
        task.setExpectedFix(revisionPolicy.cleanText(request == null ? null : request.expectedFix(), 2000));
        task.setVerificationQuery(revisionPolicy.cleanText(request == null ? null : request.verificationQuery(), 20000));
        task.setCreatedBy(revisionPolicy.cleanText(request == null ? null : request.createdBy(), 128));
        task.setAssignee(revisionPolicy.cleanText(request == null ? null : request.assignee(), 128));
        taskMapper.insert(task);
        recordEvent(feedback.getId(),
                QueryFeedbackWorkflowPolicy.EVENT_LINKED_REVISION,
                feedback.getFeedbackStatus(),
                feedback.getFeedbackStatus(),
                task.getCreatedBy(),
                "revision task created",
                Map.of("revisionNo", task.getRevisionNo(), "revisionType", task.getRevisionType()));
        workflowService.advanceToRevisionPlanned(feedback, task.getCreatedBy(), "revision task created: " + task.getRevisionNo());
        return task;
    }

    public PageResponse<RagFeedbackRevisionTask> page(Long feedbackId,
                                                      Long tenantId,
                                                      Long knowledgeBaseId,
                                                      String revisionStatus,
                                                      String assignee,
                                                      Integer pageNo,
                                                      Integer pageSize) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, "updatedAt", "DESC", MAX_PAGE_SIZE)
                .withDefaultSort("updatedAt", "DESC");
        LambdaQueryWrapper<RagFeedbackRevisionTask> query = taskQuery(feedbackId, tenantId, knowledgeBaseId, revisionStatus, assignee);
        long total = taskMapper.selectCount(query);
        LambdaQueryWrapper<RagFeedbackRevisionTask> rowsQuery = taskQuery(feedbackId, tenantId, knowledgeBaseId, revisionStatus, assignee);
        rowsQuery.orderByDesc(RagFeedbackRevisionTask::getUpdatedAt)
                .orderByDesc(RagFeedbackRevisionTask::getId)
                .last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, taskMapper.selectList(rowsQuery));
    }

    public RagFeedbackRevisionTask detail(Long id) {
        return requiredTask(id);
    }

    @Transactional
    public RagFeedbackRevisionTask apply(Long id, RagFeedbackRevisionTaskActionRequest request) {
        RagFeedbackRevisionTask task = requiredTask(id);
        revisionPolicy.requireTransition(task.getRevisionStatus(), FeedbackRevisionPolicy.STATUS_APPLIED);
        taskMapper.update(null, new LambdaUpdateWrapper<RagFeedbackRevisionTask>()
                .eq(RagFeedbackRevisionTask::getId, id)
                .set(RagFeedbackRevisionTask::getRevisionStatus, FeedbackRevisionPolicy.STATUS_APPLIED)
                .set(StringUtils.hasText(request == null ? null : request.afterSnapshotJson()),
                        RagFeedbackRevisionTask::getAfterSnapshotJson,
                        request == null ? null : request.afterSnapshotJson()));
        task.setRevisionStatus(FeedbackRevisionPolicy.STATUS_APPLIED);
        recordTaskEvent(task, request == null ? null : request.operator(), "revision applied");
        return taskMapper.selectById(id);
    }

    @Transactional
    public RagFeedbackRevisionTask verify(Long id, RagFeedbackRevisionTaskActionRequest request) {
        RagFeedbackRevisionTask task = requiredTask(id);
        String target = revisionPolicy.statusAfterVerification(request == null ? null : request.verified());
        revisionPolicy.requireTransition(task.getRevisionStatus(), target);
        taskMapper.update(null, new LambdaUpdateWrapper<RagFeedbackRevisionTask>()
                .eq(RagFeedbackRevisionTask::getId, id)
                .set(RagFeedbackRevisionTask::getRevisionStatus, target)
                .set(RagFeedbackRevisionTask::getVerificationResultJson, request == null ? null : request.verificationResultJson()));
        task.setRevisionStatus(target);
        RagQueryFeedback feedback = requiredFeedback(task.getFeedbackId());
        if (FeedbackRevisionPolicy.STATUS_VERIFIED.equals(target)) {
            workflowService.applyStatus(
                    feedback.getId(),
                    feedback.getFeedbackStatus(),
                    QueryFeedbackWorkflowPolicy.STATUS_RESOLVED,
                    request == null ? null : request.operator(),
                    request == null ? "revision verified" : request.comment(),
                    true
            );
        } else if (QueryFeedbackWorkflowPolicy.STATUS_REVISION_PLANNED.equals(feedback.getFeedbackStatus())) {
            workflowService.applyStatus(
                    feedback.getId(),
                    feedback.getFeedbackStatus(),
                    QueryFeedbackWorkflowPolicy.STATUS_IN_REVIEW,
                    request == null ? null : request.operator(),
                    request == null ? "revision verification failed" : request.comment(),
                    true
            );
        }
        recordTaskEvent(task, request == null ? null : request.operator(), "revision verified=" + Boolean.TRUE.equals(request == null ? null : request.verified()));
        return taskMapper.selectById(id);
    }

    @Transactional
    public RagFeedbackRevisionTask reject(Long id, RagFeedbackRevisionTaskActionRequest request) {
        return changeTaskStatus(id, FeedbackRevisionPolicy.STATUS_REJECTED, request);
    }

    @Transactional
    public RagFeedbackRevisionTask cancel(Long id, RagFeedbackRevisionTaskActionRequest request) {
        return changeTaskStatus(id, FeedbackRevisionPolicy.STATUS_CANCELLED, request);
    }

    private RagFeedbackRevisionTask changeTaskStatus(Long id, String target, RagFeedbackRevisionTaskActionRequest request) {
        RagFeedbackRevisionTask task = requiredTask(id);
        revisionPolicy.requireTransition(task.getRevisionStatus(), target);
        taskMapper.update(null, new LambdaUpdateWrapper<RagFeedbackRevisionTask>()
                .eq(RagFeedbackRevisionTask::getId, id)
                .set(RagFeedbackRevisionTask::getRevisionStatus, target));
        task.setRevisionStatus(target);
        recordTaskEvent(task, request == null ? null : request.operator(), request == null ? null : request.comment());
        return taskMapper.selectById(id);
    }

    private LambdaQueryWrapper<RagFeedbackRevisionTask> taskQuery(Long feedbackId,
                                                                  Long tenantId,
                                                                  Long knowledgeBaseId,
                                                                  String revisionStatus,
                                                                  String assignee) {
        LambdaQueryWrapper<RagFeedbackRevisionTask> query = new LambdaQueryWrapper<>();
        query.eq(feedbackId != null, RagFeedbackRevisionTask::getFeedbackId, feedbackId)
                .eq(tenantId != null, RagFeedbackRevisionTask::getTenantId, tenantId)
                .eq(knowledgeBaseId != null, RagFeedbackRevisionTask::getKnowledgeBaseId, knowledgeBaseId);
        
        // Only normalize and add status condition when status is provided
        if (StringUtils.hasText(revisionStatus)) {
            query.eq(RagFeedbackRevisionTask::getRevisionStatus, revisionPolicy.normalizeStatus(revisionStatus));
        }
        
        query.eq(StringUtils.hasText(assignee), RagFeedbackRevisionTask::getAssignee, assignee == null ? null : assignee.trim());
        return query;
    }

    private RagFeedbackRevisionTask requiredTask(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("revision task id is required");
        }
        RagFeedbackRevisionTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("Revision task not found: " + id);
        }
        return task;
    }

    private RagQueryFeedback requiredFeedback(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("feedback id is required");
        }
        RagQueryFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new IllegalArgumentException("Feedback not found: " + id);
        }
        return feedback;
    }

    private void recordTaskEvent(RagFeedbackRevisionTask task, String operator, String comment) {
        recordEvent(task.getFeedbackId(),
                QueryFeedbackWorkflowPolicy.EVENT_LINKED_REVISION,
                null,
                null,
                operator,
                comment,
                Map.of("revisionNo", task.getRevisionNo(), "revisionStatus", task.getRevisionStatus()));
    }

    private void recordEvent(Long feedbackId,
                             String eventType,
                             String fromStatus,
                             String toStatus,
                             String operator,
                             String comment,
                             Map<String, Object> payload) {
        RagQueryFeedbackEvent event = new RagQueryFeedbackEvent();
        event.setFeedbackId(feedbackId);
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperator(workflowPolicy.cleanOperator(operator));
        event.setComment(workflowPolicy.cleanText(comment, 2000));
        event.setPayloadJson(toJson(payload));
        eventMapper.insert(event);
    }

    private String newRevisionNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "REV-" + REVISION_NO_TIME.format(LocalDateTime.now()) + "-" + suffix;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }
}

package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.domain.rag.RagQueryFeedbackEvent;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackEventMapper;
import cc.ivera.ragdemo.mapper.RagQueryFeedbackMapper;
import cc.ivera.ragdemo.mapper.RagQueryLogMapper;
import cc.ivera.ragdemo.model.query.*;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class QueryFeedbackWorkflowService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final RagQueryFeedbackMapper feedbackMapper;
    private final RagQueryFeedbackEventMapper eventMapper;
    private final RagQueryLogMapper queryLogMapper;
    private final QueryFeedbackWorkflowPolicy policy;
    private final ObjectMapper objectMapper;

    public PageResponse<RagQueryFeedback> page(Long queryLogId,
                                               Long tenantId,
                                               String rating,
                                               String feedbackStatus,
                                               String priority,
                                               String assignee,
                                               Integer pageNo,
                                               Integer pageSize,
                                               String sortBy,
                                               String sortDirection) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        List<RagQueryFeedback> rows = feedbackMapper.selectList(new LambdaQueryWrapper<RagQueryFeedback>()
                        .eq(queryLogId != null, RagQueryFeedback::getQueryLogId, queryLogId)
                        .eq(StringUtils.hasText(rating), RagQueryFeedback::getRating, normalize(rating))
                        .eq(StringUtils.hasText(feedbackStatus), RagQueryFeedback::getFeedbackStatus, normalize(feedbackStatus))
                        .eq(StringUtils.hasText(priority), RagQueryFeedback::getPriority, normalize(priority))
                        .eq(StringUtils.hasText(assignee), RagQueryFeedback::getAssignee, clean(assignee))
                        .orderByDesc(RagQueryFeedback::getCreatedAt))
                .stream()
                .filter(feedback -> tenantId == null || queryLogTenantMatches(feedback.getQueryLogId(), tenantId))
                .toList();
        return PageResponse.slice(rows, pageQuery);
    }

    public RagQueryFeedback detail(Long id) {
        return requiredFeedback(id);
    }

    public List<RagQueryFeedbackEvent> events(Long id) {
        requiredFeedback(id);
        return eventMapper.selectList(new LambdaQueryWrapper<RagQueryFeedbackEvent>()
                .eq(RagQueryFeedbackEvent::getFeedbackId, id)
                .orderByAsc(RagQueryFeedbackEvent::getCreatedAt)
                .orderByAsc(RagQueryFeedbackEvent::getId));
    }

    @Transactional
    public RagQueryFeedback assign(Long id, RagFeedbackAssignRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        String assignee = policy.cleanText(request == null ? null : request.assignee(), 128);
        if (assignee == null) {
            throw new IllegalArgumentException("assignee must not be blank");
        }
        feedbackMapper.update(null, new LambdaUpdateWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getId, id)
                .set(RagQueryFeedback::getAssignee, assignee));
        recordEvent(id,
                QueryFeedbackWorkflowPolicy.EVENT_ASSIGNED,
                feedback.getFeedbackStatus(),
                feedback.getFeedbackStatus(),
                request == null ? null : request.operator(),
                request == null ? null : request.comment(),
                Map.of("assignee", assignee));
        return feedbackMapper.selectById(id);
    }

    @Transactional
    public RagQueryFeedback changeStatus(Long id, RagFeedbackStatusRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        String toStatus = policy.normalizeStatus(request == null ? null : request.status());
        boolean linkedRevision = request != null && Boolean.TRUE.equals(request.linkedRevision());
        String comment = request == null ? null : request.comment();
        policy.requireTransition(feedback.getFeedbackStatus(), toStatus, linkedRevision, comment);
        applyStatus(id, feedback.getFeedbackStatus(), toStatus, request == null ? null : request.operator(), comment, linkedRevision);
        return feedbackMapper.selectById(id);
    }

    @Transactional
    public RagQueryFeedback review(Long id, RagFeedbackReviewRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        policy.requireReviewAllowed(
                feedback.getFeedbackStatus(),
                request == null ? null : request.reviewResult(),
                request == null ? null : request.reviewComment()
        );
        String reviewResult = policy.normalizeReviewResult(request.reviewResult());
        String reviewComment = policy.cleanText(request.reviewComment(), 2000);
        feedbackMapper.update(null, new LambdaUpdateWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getId, id)
                .set(RagQueryFeedback::getReviewResult, reviewResult)
                .set(RagQueryFeedback::getReviewComment, reviewComment));
        recordEvent(id,
                QueryFeedbackWorkflowPolicy.EVENT_REVIEWED,
                feedback.getFeedbackStatus(),
                feedback.getFeedbackStatus(),
                request.operator(),
                reviewComment,
                Map.of("reviewResult", reviewResult));
        return feedbackMapper.selectById(id);
    }

    @Transactional
    public RagQueryFeedback comment(Long id, RagFeedbackCommentRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        String comment = policy.cleanText(request == null ? null : request.comment(), 2000);
        if (comment == null) {
            throw new IllegalArgumentException("comment must not be blank");
        }
        recordEvent(id,
                QueryFeedbackWorkflowPolicy.EVENT_COMMENTED,
                feedback.getFeedbackStatus(),
                feedback.getFeedbackStatus(),
                request == null ? null : request.operator(),
                comment,
                Map.of());
        return feedback;
    }

    @Transactional
    public RagQueryFeedback close(Long id, RagFeedbackCommentRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        policy.requireTransition(feedback.getFeedbackStatus(), QueryFeedbackWorkflowPolicy.STATUS_CLOSED, false, request == null ? null : request.comment());
        applyStatus(id,
                feedback.getFeedbackStatus(),
                QueryFeedbackWorkflowPolicy.STATUS_CLOSED,
                request == null ? null : request.operator(),
                request == null ? null : request.comment(),
                false);
        return feedbackMapper.selectById(id);
    }

    @Transactional
    public RagQueryFeedback reopen(Long id, RagFeedbackCommentRequest request) {
        RagQueryFeedback feedback = requiredFeedback(id);
        policy.requireTransition(feedback.getFeedbackStatus(), QueryFeedbackWorkflowPolicy.STATUS_OPEN, false, request == null ? null : request.comment());
        int reopened = feedback.getReopenedCount() == null ? 1 : feedback.getReopenedCount() + 1;
        feedbackMapper.update(null, new LambdaUpdateWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getId, id)
                .set(RagQueryFeedback::getFeedbackStatus, QueryFeedbackWorkflowPolicy.STATUS_OPEN)
                .set(RagQueryFeedback::getClosedAt, null)
                .set(RagQueryFeedback::getResolvedAt, null)
                .set(RagQueryFeedback::getReopenedCount, reopened));
        recordEvent(id,
                QueryFeedbackWorkflowPolicy.EVENT_STATUS_CHANGED,
                feedback.getFeedbackStatus(),
                QueryFeedbackWorkflowPolicy.STATUS_OPEN,
                request == null ? null : request.operator(),
                request == null ? null : request.comment(),
                Map.of("reopenedCount", reopened));
        return feedbackMapper.selectById(id);
    }

    void applyStatus(Long id,
                     String fromStatus,
                     String toStatus,
                     String operator,
                     String comment,
                     boolean linkedRevision) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RagQueryFeedback> update = new LambdaUpdateWrapper<RagQueryFeedback>()
                .eq(RagQueryFeedback::getId, id)
                .set(RagQueryFeedback::getFeedbackStatus, toStatus);
        if (QueryFeedbackWorkflowPolicy.STATUS_RESOLVED.equals(toStatus)) {
            update.set(RagQueryFeedback::getResolvedAt, now);
        }
        if (QueryFeedbackWorkflowPolicy.STATUS_CLOSED.equals(toStatus)) {
            update.set(RagQueryFeedback::getClosedAt, now);
        }
        feedbackMapper.update(null, update);
        recordEvent(id,
                QueryFeedbackWorkflowPolicy.STATUS_CLOSED.equals(toStatus)
                        ? QueryFeedbackWorkflowPolicy.EVENT_CLOSED
                        : QueryFeedbackWorkflowPolicy.EVENT_STATUS_CHANGED,
                fromStatus,
                toStatus,
                operator,
                comment,
                Map.of("linkedRevision", linkedRevision));
    }

    void advanceToRevisionPlanned(RagQueryFeedback feedback, String operator, String comment) {
        List<String> path = switch (StringUtils.hasText(feedback.getFeedbackStatus()) ? feedback.getFeedbackStatus() : QueryFeedbackWorkflowPolicy.STATUS_OPEN) {
            case QueryFeedbackWorkflowPolicy.STATUS_OPEN -> List.of(
                    QueryFeedbackWorkflowPolicy.STATUS_TRIAGED,
                    QueryFeedbackWorkflowPolicy.STATUS_IN_REVIEW,
                    QueryFeedbackWorkflowPolicy.STATUS_REVISION_PLANNED
            );
            case QueryFeedbackWorkflowPolicy.STATUS_TRIAGED -> List.of(
                    QueryFeedbackWorkflowPolicy.STATUS_IN_REVIEW,
                    QueryFeedbackWorkflowPolicy.STATUS_REVISION_PLANNED
            );
            case QueryFeedbackWorkflowPolicy.STATUS_IN_REVIEW -> List.of(QueryFeedbackWorkflowPolicy.STATUS_REVISION_PLANNED);
            case QueryFeedbackWorkflowPolicy.STATUS_REVISION_PLANNED -> List.of();
            default -> throw new IllegalArgumentException("Cannot create revision task from feedback status: " + feedback.getFeedbackStatus());
        };
        String from = StringUtils.hasText(feedback.getFeedbackStatus()) ? feedback.getFeedbackStatus() : QueryFeedbackWorkflowPolicy.STATUS_OPEN;
        for (String to : path) {
            applyStatus(feedback.getId(), from, to, operator, comment, true);
            from = to;
        }
    }

    private boolean queryLogTenantMatches(Long queryLogId, Long tenantId) {
        if (queryLogId == null) {
            return false;
        }
        RagQueryLog log = queryLogMapper.selectById(queryLogId);
        return log != null && Objects.equals(log.getTenantId(), tenantId);
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
        event.setOperator(policy.cleanOperator(operator));
        event.setComment(policy.cleanText(comment, 2000));
        event.setPayloadJson(toJson(payload));
        eventMapper.insert(event);
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

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

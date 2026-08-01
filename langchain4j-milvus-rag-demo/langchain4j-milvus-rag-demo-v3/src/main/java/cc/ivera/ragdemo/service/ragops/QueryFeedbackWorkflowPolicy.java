package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class QueryFeedbackWorkflowPolicy {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_TRIAGED = "TRIAGED";
    public static final String STATUS_IN_REVIEW = "IN_REVIEW";
    public static final String STATUS_REVISION_PLANNED = "REVISION_PLANNED";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    public static final String REVIEW_VALID = "VALID";
    public static final String REVIEW_INVALID = "INVALID";
    public static final String REVIEW_DUPLICATE = "DUPLICATE";
    public static final String REVIEW_NEEDS_MORE_INFO = "NEEDS_MORE_INFO";

    public static final String EVENT_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String EVENT_ASSIGNED = "ASSIGNED";
    public static final String EVENT_REVIEWED = "REVIEWED";
    public static final String EVENT_COMMENTED = "COMMENTED";
    public static final String EVENT_LINKED_REVISION = "LINKED_REVISION";
    public static final String EVENT_CLOSED = "CLOSED";

    private static final Set<String> STATUSES = Set.of(
            STATUS_OPEN,
            STATUS_TRIAGED,
            STATUS_IN_REVIEW,
            STATUS_REVISION_PLANNED,
            STATUS_RESOLVED,
            STATUS_REJECTED,
            STATUS_CLOSED
    );

    private static final Set<String> REVIEW_RESULTS = Set.of(
            REVIEW_VALID,
            REVIEW_INVALID,
            REVIEW_DUPLICATE,
            REVIEW_NEEDS_MORE_INFO
    );

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            STATUS_OPEN, Set.of(STATUS_TRIAGED, STATUS_REJECTED),
            STATUS_TRIAGED, Set.of(STATUS_IN_REVIEW),
            STATUS_IN_REVIEW, Set.of(STATUS_REVISION_PLANNED, STATUS_RESOLVED, STATUS_REJECTED),
            STATUS_REVISION_PLANNED, Set.of(STATUS_RESOLVED, STATUS_IN_REVIEW),
            STATUS_RESOLVED, Set.of(STATUS_CLOSED, STATUS_IN_REVIEW),
            STATUS_REJECTED, Set.of(STATUS_CLOSED, STATUS_IN_REVIEW),
            STATUS_CLOSED, Set.of(STATUS_OPEN)
    );

    public String defaultStatus(String rating) {
        String normalized = normalize(rating);
        return QueryFeedbackPolicy.RATING_NOT_HELPFUL.equals(normalized) ? STATUS_TRIAGED : STATUS_OPEN;
    }

    public String defaultPriority(String rating) {
        String normalized = normalize(rating);
        return QueryFeedbackPolicy.RATING_CORRECTION.equals(normalized) ? PRIORITY_HIGH : PRIORITY_MEDIUM;
    }

    public String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("feedback status must not be blank");
        }
        String normalized = normalize(status);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported feedback status: " + status);
        }
        return normalized;
    }

    public String normalizeReviewResult(String reviewResult) {
        if (!StringUtils.hasText(reviewResult)) {
            throw new IllegalArgumentException("reviewResult must not be blank");
        }
        String normalized = normalize(reviewResult);
        if (!REVIEW_RESULTS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported review result: " + reviewResult);
        }
        return normalized;
    }

    public void requireTransition(String fromStatus, String toStatus, boolean linkedRevision, String comment) {
        String from = StringUtils.hasText(fromStatus) ? normalizeStatus(fromStatus) : STATUS_OPEN;
        String to = normalizeStatus(toStatus);
        if (from.equals(to)) {
            return;
        }
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Illegal feedback status transition: " + from + " -> " + to);
        }
        if (STATUS_RESOLVED.equals(to) && !linkedRevision && !StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("Resolving feedback requires a linked revision or explicit resolution comment");
        }
    }

    public void requireReviewAllowed(String status, String reviewResult, String reviewComment) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeStatus(status) : STATUS_OPEN;
        if (!Set.of(STATUS_IN_REVIEW, STATUS_REVISION_PLANNED, STATUS_RESOLVED, STATUS_REJECTED, STATUS_CLOSED).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Review is allowed only from IN_REVIEW or later statuses");
        }
        normalizeReviewResult(reviewResult);
        if (!StringUtils.hasText(reviewComment)) {
            throw new IllegalArgumentException("reviewComment must not be blank");
        }
    }

    public String cleanText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (maxLength <= 0) {
            return "";
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public String cleanOperator(String operator) {
        String cleaned = cleanText(operator, 128);
        return cleaned == null ? "system" : cleaned;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}

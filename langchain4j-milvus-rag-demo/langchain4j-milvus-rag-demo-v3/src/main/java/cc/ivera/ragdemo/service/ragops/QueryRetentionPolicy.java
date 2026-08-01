package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.RagQueryLog;
import cc.ivera.ragdemo.domain.rag.RagQueryRetentionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class QueryRetentionPolicy {

    @Autowired
    public QueryRetentionPolicy() {
    }

    public static final String ARCHIVE_ACTIVE = "ACTIVE";
    public static final String ARCHIVE_ARCHIVED = "ARCHIVED";
    public static final String ARCHIVE_DELETE_PENDING = "DELETE_PENDING";
    public static final String ARCHIVE_DELETED = "DELETED";

    private static final int SUCCESS_RETENTION_DAYS = 180;
    private static final int FAILED_RETENTION_DAYS = 365;
    private static final int LONG_TERM_RETENTION_DAYS = 3650;

    public LocalDateTime retentionUntil(RagQueryLog log,
                                        List<RagQueryRetentionPolicy> policies,
                                        boolean hasFeedback,
                                        boolean hasCorrection,
                                        LocalDateTime now) {
        int days = retentionDays(log, policies, hasFeedback, hasCorrection);
        return (now == null ? LocalDateTime.now() : now).plusDays(days);
    }

    public int retentionDays(RagQueryLog log,
                             List<RagQueryRetentionPolicy> policies,
                             boolean hasFeedback,
                             boolean hasCorrection) {
        if (hasFeedback || hasCorrection) {
            return LONG_TERM_RETENTION_DAYS;
        }
        return policies == null ? defaultDays(log) : policies.stream()
                .filter(policy -> matches(policy, log))
                .max(Comparator.comparing(policy -> policy.getUpdatedAt() == null ? LocalDateTime.MIN : policy.getUpdatedAt()))
                .map(RagQueryRetentionPolicy::getRetentionDays)
                .filter(days -> days != null && days >= 0)
                .orElse(defaultDays(log));
    }

    public boolean canPurge(RagQueryLog log, boolean hasFeedback, LocalDateTime now) {
        if (log == null || !Boolean.TRUE.equals(log.getDeleted())) {
            return false;
        }
        if (hasFeedback) {
            return false;
        }
        LocalDateTime threshold = log.getRetentionUntil();
        return threshold == null || !threshold.isAfter(now == null ? LocalDateTime.now() : now);
    }

    public String normalizeArchiveStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return ARCHIVE_ACTIVE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case ARCHIVE_ACTIVE, ARCHIVE_ARCHIVED, ARCHIVE_DELETE_PENDING, ARCHIVE_DELETED -> normalized;
            default -> ARCHIVE_ACTIVE;
        };
    }

    public boolean matches(RagQueryRetentionPolicy policy, RagQueryLog log) {
        if (policy == null || log == null || Boolean.FALSE.equals(policy.getEnabled())) {
            return false;
        }
        boolean tenantMatches = policy.getTenantId() == null || policy.getTenantId() == 0L || Objects.equals(policy.getTenantId(), log.getTenantId());
        return tenantMatches
                && valueMatches(policy.getQueryType(), log.getQueryType(), "ALL")
                && valueMatches(policy.getStatusFilter(), log.getStatus(), "ALL");
    }

    private boolean valueMatches(String policyValue, String actual, String wildcard) {
        if (!StringUtils.hasText(policyValue)) {
            return true;
        }
        String normalized = policyValue.trim().toUpperCase(Locale.ROOT);
        return wildcard.equals(normalized) || normalized.equals(normalize(actual));
    }

    private int defaultDays(RagQueryLog log) {
        String status = log == null ? null : normalize(log.getStatus());
        return QueryAuditPolicy.STATUS_FAILED.equals(status) ? FAILED_RETENTION_DAYS : SUCCESS_RETENTION_DAYS;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}

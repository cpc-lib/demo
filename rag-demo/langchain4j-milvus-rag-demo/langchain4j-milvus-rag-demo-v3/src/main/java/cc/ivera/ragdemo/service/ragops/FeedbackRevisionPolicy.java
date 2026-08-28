package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FeedbackRevisionPolicy {

    public static final String TYPE_UPDATE_CHUNK = "UPDATE_CHUNK";
    public static final String TYPE_ADD_DOCUMENT = "ADD_DOCUMENT";
    public static final String TYPE_DISABLE_CHUNK = "DISABLE_CHUNK";
    public static final String TYPE_REPARSE_DOCUMENT = "REPARSE_DOCUMENT";
    public static final String TYPE_OTHER = "OTHER";

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> TYPES = Set.of(
            TYPE_UPDATE_CHUNK,
            TYPE_ADD_DOCUMENT,
            TYPE_DISABLE_CHUNK,
            TYPE_REPARSE_DOCUMENT,
            TYPE_OTHER
    );

    private static final Set<String> STATUSES = Set.of(
            STATUS_PLANNED,
            STATUS_IN_PROGRESS,
            STATUS_APPLIED,
            STATUS_VERIFIED,
            STATUS_REJECTED,
            STATUS_CANCELLED
    );

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            STATUS_PLANNED, Set.of(STATUS_IN_PROGRESS, STATUS_APPLIED, STATUS_REJECTED, STATUS_CANCELLED),
            STATUS_IN_PROGRESS, Set.of(STATUS_APPLIED, STATUS_REJECTED, STATUS_CANCELLED),
            STATUS_APPLIED, Set.of(STATUS_VERIFIED, STATUS_IN_PROGRESS, STATUS_REJECTED),
            STATUS_VERIFIED, Set.of(),
            STATUS_REJECTED, Set.of(),
            STATUS_CANCELLED, Set.of()
    );

    public String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return TYPE_OTHER;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalized)) {
            return TYPE_OTHER;
        }
        return normalized;
    }

    public String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("revision status must not be blank");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported revision status: " + status);
        }
        return normalized;
    }

    public void requireTransition(String fromStatus, String toStatus) {
        String from = StringUtils.hasText(fromStatus) ? normalizeStatus(fromStatus) : STATUS_PLANNED;
        String to = normalizeStatus(toStatus);
        if (from.equals(to)) {
            return;
        }
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Illegal revision task transition: " + from + " -> " + to);
        }
    }

    public String statusAfterVerification(Boolean verified) {
        return Boolean.TRUE.equals(verified) ? STATUS_VERIFIED : STATUS_IN_PROGRESS;
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
}

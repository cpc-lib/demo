package cc.ivera.ragdemo.service.ragops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class QueryDeletePolicy {

    @Autowired
    public QueryDeletePolicy() {
    }

    public static final String MODE_SOFT_DELETE = "SOFT_DELETE";
    public static final String MODE_ARCHIVE = "ARCHIVE";
    public static final String MODE_PURGE = "PURGE";
    public static final String MODE_RESTORE = "RESTORE";

    private static final DateTimeFormatter DELETE_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public List<Long> cleanIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    public String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return MODE_SOFT_DELETE;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case MODE_SOFT_DELETE, MODE_ARCHIVE, MODE_PURGE, MODE_RESTORE -> normalized;
            default -> throw new IllegalArgumentException("Unsupported query log operation mode: " + mode);
        };
    }

    public String cleanOperator(String operator) {
        return cleanText(operator, 128, "system");
    }

    public String cleanReason(String reason) {
        return cleanText(reason, 1000, null);
    }

    public String newDeleteNo(String mode) {
        return newDeleteNo(mode, Clock.systemDefaultZone());
    }

    public String newDeleteNo(String mode, Clock clock) {
        String prefix = switch (normalizeMode(mode)) {
            case MODE_ARCHIVE -> "ARCH";
            case MODE_PURGE -> "PURGE";
            case MODE_RESTORE -> "RESTORE";
            default -> "DEL";
        };
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return prefix + "-" + DELETE_NO_TIME.format(LocalDateTime.now(clock)) + "-" + suffix;
    }

    private String cleanText(String value, int maxLength, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String trimmed = value.trim();
        if (maxLength <= 0) {
            return "";
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}

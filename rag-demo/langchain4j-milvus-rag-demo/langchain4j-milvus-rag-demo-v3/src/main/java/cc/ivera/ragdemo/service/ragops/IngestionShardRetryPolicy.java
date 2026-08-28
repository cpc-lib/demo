package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.IngestionShardStatus;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

public final class IngestionShardRetryPolicy {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private static final List<Duration> BACKOFFS = List.of(
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10)
    );
    private static final List<String> RETRYABLE_MARKERS = List.of(
            "timeout",
            "timed out",
            "rate limit",
            "too many requests",
            "429",
            "temporarily unavailable",
            "connection reset",
            "connection refused",
            "network",
            "milvus unavailable"
    );
    private static final List<String> FINAL_MARKERS = List.of(
            "dimension",
            "corrupt",
            "unsupported",
            "schema",
            "permission",
            "unauthorized",
            "forbidden"
    );

    private IngestionShardRetryPolicy() {
    }

    public static boolean isRetryable(Throwable error) {
        if (error == null) {
            return false;
        }
        if (error instanceof SocketTimeoutException || error instanceof TimeoutException) {
            return true;
        }
        if (error instanceof IOException) {
            return true;
        }
        String message = errorMessage(error);
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (FINAL_MARKERS.stream().anyMatch(normalized::contains)) {
            return false;
        }
        return RETRYABLE_MARKERS.stream().anyMatch(normalized::contains);
    }

    public static IngestionShardStatus failureStatus(Throwable error, Integer retryCount, Integer maxRetryCount) {
        int nextRetryCount = safeRetryCount(retryCount) + 1;
        if (isRetryable(error) && nextRetryCount <= safeMaxRetryCount(maxRetryCount)) {
            return IngestionShardStatus.FAILED_RETRYABLE;
        }
        return IngestionShardStatus.FAILED_FINAL;
    }

    public static LocalDateTime nextRetryAt(LocalDateTime baseTime, Integer retryCount) {
        LocalDateTime base = baseTime == null ? LocalDateTime.now() : baseTime;
        int index = Math.min(Math.max(safeRetryCount(retryCount), 0), BACKOFFS.size() - 1);
        return base.plus(BACKOFFS.get(index));
    }

    public static int nextRetryCount(Integer retryCount) {
        return safeRetryCount(retryCount) + 1;
    }

    public static int defaultMaxRetryCount() {
        return DEFAULT_MAX_RETRY_COUNT;
    }

    public static String errorCode(Throwable error) {
        if (error == null) {
            return "UNKNOWN";
        }
        if (isRetryable(error)) {
            return "RETRYABLE_ERROR";
        }
        return "FINAL_ERROR";
    }

    public static String errorMessage(Throwable error, int maxLength) {
        String message = errorMessage(error);
        if (!StringUtils.hasText(message) || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength);
    }

    private static String errorMessage(Throwable error) {
        if (error == null) {
            return null;
        }
        if (StringUtils.hasText(error.getMessage())) {
            return error.getMessage();
        }
        return error.getClass().getSimpleName();
    }

    private static int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : Math.max(0, retryCount);
    }

    private static int safeMaxRetryCount(Integer maxRetryCount) {
        return maxRetryCount == null ? DEFAULT_MAX_RETRY_COUNT : Math.max(0, maxRetryCount);
    }
}

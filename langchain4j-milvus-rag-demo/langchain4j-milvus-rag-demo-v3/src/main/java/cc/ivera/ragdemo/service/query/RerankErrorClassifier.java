package cc.ivera.ragdemo.service.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RerankErrorClassifier {

    @Autowired
    public RerankErrorClassifier() {
    }

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP\\s+(\\d{3})");

    public Classification classify(boolean success,
                                   boolean fallback,
                                   String errorCode,
                                   String errorMessage) {
        Integer httpStatus = httpStatus(errorCode, errorMessage);
        String normalized = normalize(success, fallback, errorCode, errorMessage, httpStatus);
        return new Classification(httpStatus, normalized, degradedReason(normalized, fallback));
    }

    public Classification classify(Throwable throwable) {
        if (throwable == null) {
            return new Classification(null, null, null);
        }
        String normalized = normalizeThrowable(throwable);
        return new Classification(httpStatus(throwable.getMessage(), null), normalized, degradedReason(normalized, true));
    }

    private String normalize(boolean success,
                             boolean fallback,
                             String errorCode,
                             String errorMessage,
                             Integer httpStatus) {
        if (success && !fallback && !StringUtils.hasText(errorCode)) {
            return null;
        }
        if ("EMPTY_RERANK_RESULT".equalsIgnoreCase(errorCode)) {
            return "EMPTY_RESULT";
        }
        if (httpStatus != null) {
            if (httpStatus == 401 || httpStatus == 403) {
                return "AUTH_FAILED";
            }
            if (httpStatus == 429) {
                return "RATE_LIMITED";
            }
            if (httpStatus >= 500) {
                return "PROVIDER_5XX";
            }
        }
        String combined = ((errorCode == null ? "" : errorCode) + " " + (errorMessage == null ? "" : errorMessage))
                .toLowerCase(Locale.ROOT);
        if (combined.contains("timeout") || combined.contains("timed out")) {
            return "TIMEOUT";
        }
        if (combined.contains("json") || combined.contains("parse")) {
            return "INVALID_RESPONSE";
        }
        if (combined.contains("connect") || combined.contains("connection") || combined.contains("network")) {
            return "NETWORK_ERROR";
        }
        if (fallback || StringUtils.hasText(errorCode) || StringUtils.hasText(errorMessage)) {
            return "API_ERROR";
        }
        return null;
    }

    private String normalizeThrowable(Throwable throwable) {
        if (throwable instanceof HttpTimeoutException || throwable instanceof SocketTimeoutException) {
            return "TIMEOUT";
        }
        if (throwable instanceof ConnectException) {
            return "NETWORK_ERROR";
        }
        return normalize(false, true, throwable.getClass().getSimpleName(), throwable.getMessage(), httpStatus(throwable.getMessage(), null));
    }

    private Integer httpStatus(String errorCode, String errorMessage) {
        Integer fromCode = parseStatus(errorCode);
        if (fromCode != null) {
            return fromCode;
        }
        return parseStatus(errorMessage);
    }

    private Integer parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        String trimmed = value.trim();
        if (trimmed.matches("\\d{3}")) {
            return Integer.parseInt(trimmed);
        }
        return null;
    }

    private String degradedReason(String normalized, boolean fallback) {
        if (!fallback && !StringUtils.hasText(normalized)) {
            return null;
        }
        if (!StringUtils.hasText(normalized)) {
            return "fallback";
        }
        return switch (normalized) {
            case "TIMEOUT" -> "timeout";
            case "RATE_LIMITED" -> "rate_limit";
            case "EMPTY_RESULT" -> "empty_result";
            case "AUTH_FAILED" -> "auth_failed";
            case "NETWORK_ERROR" -> "network_error";
            case "INVALID_RESPONSE" -> "invalid_response";
            default -> "api_error";
        };
    }

    public record Classification(
            Integer httpStatus,
            String errorCodeNormalized,
            String degradedReason
    ) {
    }
}

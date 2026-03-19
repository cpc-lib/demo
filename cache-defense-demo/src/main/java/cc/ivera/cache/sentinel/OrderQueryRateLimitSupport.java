package cc.ivera.cache.sentinel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class OrderQueryRateLimitSupport {

    private static final String UNKNOWN_IP = "unknown";

    private OrderQueryRateLimitSupport() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedIp = firstValidIp(request.getHeader("X-Forwarded-For"));
        if (forwardedIp != null) {
            return forwardedIp;
        }

        String realIp = firstValidIp(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        if (StringUtils.hasText(request.getRemoteAddr())) {
            return request.getRemoteAddr().trim();
        }
        return UNKNOWN_IP;
    }

    public static String buildLimitKey(String clientIp, Long orderId) {
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp.trim() : UNKNOWN_IP;
        return normalizedIp + ":" + String.valueOf(orderId);
    }

    private static String firstValidIp(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }

        for (String candidate : headerValue.split(",")) {
            String ip = candidate.trim();
            if (StringUtils.hasText(ip) && !UNKNOWN_IP.equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }
}

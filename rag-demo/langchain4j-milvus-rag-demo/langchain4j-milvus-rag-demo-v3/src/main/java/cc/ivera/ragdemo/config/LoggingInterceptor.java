package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 日志拦截器，负责设置 MDC (Mapped Diagnostic Context) 上下文信息
 * 包含 traceId、tenantId、userId 等，用于统一日志格式和链路追踪
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    /**
     * MDC key: 请求追踪ID
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * MDC key: 租户ID
     */
    public static final String MDC_TENANT_ID = "tenantId";

    /**
     * MDC key: 用户ID
     */
    public static final String MDC_USER_ID = "userId";

    /**
     * MDC key: 请求URI
     */
    public static final String MDC_REQUEST_URI = "requestUri";

    /**
     * MDC key: HTTP方法
     */
    public static final String MDC_HTTP_METHOD = "httpMethod";

    /**
     * 请求头中 traceId 的名称
     */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求头中 requestId 的名称（备选）
     */
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 设置 traceId（优先从请求头获取，否则生成新的）
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(HEADER_REQUEST_ID);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);

        // 设置请求URI和HTTP方法
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
        MDC.put(MDC_HTTP_METHOD, request.getMethod());

        // 尝试从 TenantContextHolder 获取租户ID和用户ID
        TenantContextHolder.current().ifPresent(context -> {
            if (context.tenantId() != null) {
                MDC.put(MDC_TENANT_ID, context.tenantId().toString());
            }
            if (context.user() != null && context.user().userId() != null) {
                MDC.put(MDC_USER_ID, context.user().userId());
            }
        });

        // 将 traceId 写入响应头，方便客户端追踪
        response.setHeader(HEADER_TRACE_ID, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清空 MDC，避免线程复用导致上下文污染
        MDC.clear();
    }
}
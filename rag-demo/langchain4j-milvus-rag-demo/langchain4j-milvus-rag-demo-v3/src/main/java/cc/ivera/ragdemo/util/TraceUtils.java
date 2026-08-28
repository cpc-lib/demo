package cc.ivera.ragdemo.util;

import org.slf4j.MDC;

/**
 * 链路追踪工具类
 * 统一 traceId 的获取和生成逻辑
 * Micrometer Tracing 自动将 traceId 写入 MDC，本工具从中读取
 */
public final class TraceUtils {

    /** MDC 中 traceId 的 key（Micrometer Tracing 标准键名） */
    private static final String TRACE_ID_KEY = "traceId";
    /** MDC 中 spanId 的 key */
    private static final String SPAN_ID_KEY = "spanId";

    private TraceUtils() {
    }

    /**
     * 获取当前请求的 traceId
     * 优先从 MDC 读取（Micrometer Tracing 自动写入），为空时生成新的
     */
    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return newTraceId();
    }

    /**
     * 生成新的 traceId（与 RagApiResponse.newTraceId 保持一致的格式）
     */
    public static String newTraceId() {
        return "trace_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取当前 spanId
     */
    public static String currentSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }

    /**
     * 判断当前是否有活跃的 trace
     */
    public static boolean hasTrace() {
        String traceId = MDC.get(TRACE_ID_KEY);
        return traceId != null && !traceId.isBlank();
    }

    /**
     * 将 traceId 写入 MDC（用于异步线程手动传播）
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除 MDC 中的 traceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }
}

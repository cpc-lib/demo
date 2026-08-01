package cc.ivera.ragdemo.util;

import java.util.Locale;
import java.util.Map;

/**
 * 字符串处理工具类
 * 统一项目中分散的字符串处理逻辑，包括：
 * - stringValue: Object 安全转 String
 * - truncate: 字符串截断
 * - firstNonBlank: 返回第一个非空白字符串
 * - nullToEmpty / emptyToNull: null 与空字符串转换
 * - normalize: 规范化字符串（trim + uppercase）
 * - escapeJson: JSON 字符串转义
 * - stripMarkdownFence: 去除 Markdown 代码围栏
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 将 Object 安全转为 String，null 返回 null
     */
    public static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 将 Object 安全转为 String，null 返回空字符串
     */
    public static String stringValueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 将 Object 安全转为 String，null 或空白返回默认值
     */
    public static String stringValueOrDefault(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String str = String.valueOf(value);
        return str.isBlank() ? defaultValue : str;
    }

    /**
     * 截断字符串到指定长度
     *
     * @param value     原始字符串，null 返回 null
     * @param maxLength 最大长度，<=0 返回空字符串
     * @return 截断后的字符串
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 返回第一个非空白字符串
     */
    public static String firstNonBlank(String first, String second) {
        return isNotBlank(first) ? first : second;
    }

    /**
     * 返回第一个非空白字符串，都为空时返回 null
     */
    public static String firstNonBlankOrNull(String first, String second) {
        String result = firstNonBlank(first, second);
        return isBlank(result) ? null : result;
    }

    /**
     * null 转空字符串
     */
    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 空字符串转 null
     */
    public static String emptyToNull(String value) {
        return isBlank(value) ? null : value;
    }

    /**
     * 规范化字符串：trim 后转大写
     */
    public static String normalize(String value) {
        if (isBlank(value)) {
            return value;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化字符串：trim 后转小写
     */
    public static String normalizeLower(String value) {
        if (isBlank(value)) {
            return value;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断字符串是否为空白（null、空串或纯空格）
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 判断字符串是否非空白
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /**
     * 转义 JSON 字符串中的特殊字符（反斜杠和双引号）
     */
    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 转义 JSON 字符串并加上双引号包裹
     */
    public static String quote(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    /**
     * 去除 Markdown 代码围栏（```json ... ``` 或 ``` ... ```）
     */
    public static String stripMarkdownFence(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    /**
     * 清理文本：null/blank 返回 null，否则 trim
     */
    public static String cleanText(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 清理文本并截断到指定长度
     */
    public static String cleanText(String value, int maxLength) {
        String cleaned = cleanText(value);
        return truncate(cleaned, maxLength);
    }

    /**
     * 将 Map 的键安全转为 String 类型
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> safeToStringKeyMap(Map<?, ?> map) {
        if (map == null) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * 将 long 安全转为 String，null 返回 null
     */
    public static String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}

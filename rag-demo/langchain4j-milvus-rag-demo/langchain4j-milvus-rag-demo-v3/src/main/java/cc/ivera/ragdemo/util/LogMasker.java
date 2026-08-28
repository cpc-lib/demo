package cc.ivera.ragdemo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具类
 * 对日志中的敏感信息进行脱敏处理，防止敏感数据泄露
 */
public final class LogMasker {

    /** 脱敏替换字符串 */
    private static final String MASK = "****";

    /** 敏感字段名集合 */
    private static final Set<String> SENSITIVE_FIELD_NAMES = new HashSet<>();

    /** API Key 模式匹配 */
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "(sk-[a-zA-Z0-9_\\-]{10,})|(api[_\\-]?key[=\\s:]+[a-zA-Z0-9_\\-]{10,})|(secret[_\\-]?key[=\\s:]+[a-zA-Z0-9_\\-]{10,})",
            Pattern.CASE_INSENSITIVE
    );

    /** Token 模式匹配 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(Bearer\\s+[a-zA-Z0-9_\\-]{20,})|(token[=\\s:]+[a-zA-Z0-9_\\-]{20,})",
            Pattern.CASE_INSENSITIVE
    );

    /** Base64 编码数据模式（可能包含敏感信息） */
    private static final Pattern BASE64_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+/]{32,}={0,2}",
            Pattern.CASE_INSENSITIVE
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 初始化敏感字段名
        SENSITIVE_FIELD_NAMES.add("apikey");
        SENSITIVE_FIELD_NAMES.add("api_key");
        SENSITIVE_FIELD_NAMES.add("api-key");
        SENSITIVE_FIELD_NAMES.add("secret");
        SENSITIVE_FIELD_NAMES.add("secret_key");
        SENSITIVE_FIELD_NAMES.add("secret-key");
        SENSITIVE_FIELD_NAMES.add("token");
        SENSITIVE_FIELD_NAMES.add("access_token");
        SENSITIVE_FIELD_NAMES.add("refresh_token");
        SENSITIVE_FIELD_NAMES.add("password");
        SENSITIVE_FIELD_NAMES.add("passwd");
        SENSITIVE_FIELD_NAMES.add("credential");
        SENSITIVE_FIELD_NAMES.add("credentials");
        SENSITIVE_FIELD_NAMES.add("authorization");
        SENSITIVE_FIELD_NAMES.add("auth");
        SENSITIVE_FIELD_NAMES.add("key");
    }

    private LogMasker() {
    }

    /**
     * 脱敏 HTTP 响应体，自动检测格式并进行脱敏处理
     *
     * @param body HTTP 响应体
     * @return 脱敏后的响应体
     */
    public static String maskResponseBody(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        // 尝试按 JSON 格式处理
        if (isJson(body)) {
            return maskJson(body);
        }
        // 普通字符串处理
        return maskText(body);
    }

    /**
     * 判断字符串是否为 JSON 格式
     */
    private static boolean isJson(String body) {
        String trimmed = body.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * 对 JSON 字符串进行脱敏处理
     */
    private static String maskJson(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            maskJsonNode(root);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            // JSON 解析失败，降级为普通文本脱敏
            return maskText(json);
        }
    }

    /**
     * 递归脱敏 JSON 节点
     */
    private static void maskJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            java.util.Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (isSensitiveField(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskJsonNode(objectNode.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                maskJsonNode(item);
            }
        }
        // 对于基本类型，不做处理（敏感字段已在父级对象中处理）
    }

    /**
     * 判断字段名是否为敏感字段
     */
    private static boolean isSensitiveField(String fieldName) {
        return fieldName != null && SENSITIVE_FIELD_NAMES.contains(fieldName.toLowerCase());
    }

    /**
     * 对普通文本进行脱敏处理
     */
    private static String maskText(String text) {
        String result = text;
        // 脱敏 API Key
        result = maskApiKey(result);
        // 脱敏 Token
        result = maskToken(result);
        // 脱敏长 Base64（可能包含敏感信息）
        result = maskLongBase64(result);
        return result;
    }

    /**
     * 脱敏 API Key
     */
    private static String maskApiKey(String text) {
        Matcher matcher = API_KEY_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            // 保留前缀（如 sk-），脱敏后面部分
            if (match.startsWith("sk-")) {
                matcher.appendReplacement(sb, "sk-" + MASK);
            } else {
                matcher.appendReplacement(sb, MASK);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏 Token
     */
    private static String maskToken(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            if (match.toLowerCase().startsWith("bearer")) {
                matcher.appendReplacement(sb, "Bearer " + MASK);
            } else {
                matcher.appendReplacement(sb, MASK);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 脱敏长 Base64 编码数据（短的可能是正常的 ID）
     */
    private static String maskLongBase64(String text) {
        Matcher matcher = BASE64_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group();
            // 只脱敏长度超过64的 Base64（可能包含敏感信息）
            if (match.length() > 64) {
                // 保留前8位和后8位
                String prefix = match.substring(0, 8);
                String suffix = match.substring(match.length() - 8);
                matcher.appendReplacement(sb, prefix + MASK + suffix);
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 截取并脱敏字符串（用于过长的响应体）
     *
     * @param body     响应体
     * @param maxLength 最大长度
     * @return 截取并脱敏后的字符串
     */
    public static String truncateAndMask(String body, int maxLength) {
        if (body == null) {
            return null;
        }
        String masked = maskResponseBody(body);
        if (masked.length() <= maxLength) {
            return masked;
        }
        return masked.substring(0, maxLength) + "...[truncated]";
    }

    /**
     * 截取并脱敏字符串（默认最大长度 2000）
     */
    public static String truncateAndMask(String body) {
        return truncateAndMask(body, 2000);
    }
}

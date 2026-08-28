package cc.ivera.ragdemo.util;

/**
 * API Key 日志脱敏工具类
 * 在日志中屏蔽 API Key 敏感信息，仅显示前几位和后几位
 */
public final class ApiKeyMasker {

    /** 前缀显示长度 */
    private static final int PREFIX_LENGTH = 4;
    /** 后缀显示长度 */
    private static final int SUFFIX_LENGTH = 4;
    /** 最短脱敏长度（短于此长度则完全遮盖） */
    private static final int MIN_MASK_LENGTH = 12;

    private ApiKeyMasker() {
    }

    /**
     * 脱敏 API Key，仅显示前4位和后4位
     * 示例：sk-abc123456789xyz -> sk-a****xyz
     *
     * @param apiKey 明文 API Key
     * @return 脱敏后的字符串
     */
    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return apiKey;
        }
        if (apiKey.length() < MIN_MASK_LENGTH) {
            return "****";
        }
        int prefix = Math.min(PREFIX_LENGTH, apiKey.length() / 4);
        int suffix = Math.min(SUFFIX_LENGTH, apiKey.length() / 4);
        return apiKey.substring(0, prefix) + "****" + apiKey.substring(apiKey.length() - suffix);
    }

    /**
     * 判断字符串是否可能包含 API Key（用于日志拦截）
     *
     * @param value 待检查的字符串
     * @return true 如果字符串包含 apiKey/api_key/secret 等关键词
     */
    public static boolean looksLikeApiKey(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.contains("apikey") || lower.contains("api_key")
                || lower.contains("api-key") || lower.contains("secret")
                || lower.contains("bearer ");
    }
}

package cc.ivera.ragdemo.service.ragops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class RetrievalQueryClassificationPolicy {

    @Autowired
    public RetrievalQueryClassificationPolicy() {
    }

    public String category(String query, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback.trim().toLowerCase(Locale.ROOT);
        }
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return "definition";
        }
        if (containsAny(normalized, "怎么", "如何", "how to", "步骤", "流程")) {
            return "how_to";
        }
        if (containsAny(normalized, "区别", "对比", "比较", "compare", "difference", "vs")) {
            return "comparison";
        }
        if (containsAny(normalized, "多少", "统计", "数量", "number", "count", "rate", "比例")) {
            return "numeric";
        }
        if (containsAny(normalized, "报错", "异常", "错误", "失败", "error", "exception", "failed", "timeout")) {
            return "troubleshooting";
        }
        if (containsAny(normalized, "图片", "图像", "截图", "image", "figure", "chart")) {
            return "multimodal";
        }
        return "definition";
    }

    public String language(String query, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback.trim().toLowerCase(Locale.ROOT);
        }
        if (!StringUtils.hasText(query)) {
            return "mixed";
        }
        boolean hasCjk = false;
        boolean hasAsciiWord = false;
        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            if (isCjk(ch)) {
                hasCjk = true;
            } else if (ch < 128 && Character.isLetter(ch)) {
                hasAsciiWord = true;
            }
        }
        if (hasCjk && hasAsciiWord) {
            return "mixed";
        }
        if (hasCjk) {
            return "zh";
        }
        return hasAsciiWord ? "en" : "mixed";
    }

    public String difficulty(String query, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback.trim().toLowerCase(Locale.ROOT);
        }
        int length = query == null ? 0 : query.trim().length();
        if (length > 80 || containsAny(normalize(query), "为什么", "根因", "排查", "tradeoff", "architecture")) {
            return "hard";
        }
        if (length > 30 || containsAny(normalize(query), "对比", "比较", "how to", "步骤")) {
            return "medium";
        }
        return "easy";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}

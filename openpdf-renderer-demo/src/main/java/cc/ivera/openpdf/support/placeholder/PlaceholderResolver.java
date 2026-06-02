package cc.ivera.openpdf.support.placeholder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单占位符解析器：${key}
 */
public class PlaceholderResolver {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public String resolve(String template, Map<String, Object> placeholders) {
        if (template == null || template.isBlank()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }

        Matcher matcher = PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = placeholders.get(key);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}

package cc.ivera.migration.util;

import java.util.regex.Pattern;

public final class SqlGuard {
    private SqlGuard() {}
    public static String safeName(String value, String regex) {
        if (value == null || !Pattern.matches(regex, value)) {
            throw new IllegalArgumentException("非法表名/字段名: " + value);
        }
        return value;
    }
}

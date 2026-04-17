package org.apache.commons.lang3.time;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 仅提供当前项目实际用到的最小能力，避免为了两个方法引入整个 commons-lang3。
 */
public final class DateFormatUtils {

    private DateFormatUtils() {
    }

    public static String format(Date date, String pattern) {
        if (date == null) {
            throw new IllegalArgumentException("date 不能为空");
        }
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("pattern 不能为空");
        }
        return new SimpleDateFormat(pattern).format(date);
    }
}

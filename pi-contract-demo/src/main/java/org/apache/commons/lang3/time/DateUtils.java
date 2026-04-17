package org.apache.commons.lang3.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 仅提供当前项目继承后实际依赖的 parseDate 能力。
 */
public class DateUtils {

    protected DateUtils() {
    }

    public static Date parseDate(String str, String... parsePatterns) throws ParseException {
        if (str == null) {
            throw new IllegalArgumentException("待解析日期不能为空");
        }
        if (parsePatterns == null || parsePatterns.length == 0) {
            throw new IllegalArgumentException("parsePatterns 不能为空");
        }

        ParseException last = null;
        for (String parsePattern : parsePatterns) {
            if (parsePattern == null || parsePattern.isBlank()) {
                continue;
            }
            try {
                SimpleDateFormat format = new SimpleDateFormat(parsePattern);
                format.setLenient(false);
                return format.parse(str);
            } catch (ParseException e) {
                last = e;
            }
        }

        if (last != null) {
            throw last;
        }
        throw new ParseException("无法解析日期: " + str, 0);
    }
}

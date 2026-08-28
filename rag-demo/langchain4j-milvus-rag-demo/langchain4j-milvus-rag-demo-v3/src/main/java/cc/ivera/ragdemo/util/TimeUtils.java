package cc.ivera.ragdemo.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间处理工具类
 * 统一项目中分散的时间格式化和类型转换逻辑
 */
public final class TimeUtils {

    /** 文件名时间戳格式：yyyyMMddHHmmss */
    public static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** ISO 8601 偏移时间格式 */
    public static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private TimeUtils() {
    }

    /**
     * 生成当前时间的文件名时间戳字符串（yyyyMMddHHmmss）
     */
    public static String filenameTimestamp() {
        return FILENAME_TIMESTAMP.format(LocalDateTime.now());
    }

    /**
     * 生成指定时间的文件名时间戳字符串
     */
    public static String filenameTimestamp(LocalDateTime dateTime) {
        return FILENAME_TIMESTAMP.format(dateTime);
    }

    /**
     * 获取当前时间的 ISO 8601 偏移时间戳字符串
     */
    public static String isoOffsetNow() {
        return OffsetDateTime.now().toString();
    }

    /**
     * 将 Instant 转为 LocalDateTime（使用系统默认时区）
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * 将 LocalDateTime 转为 Instant（使用系统默认时区）
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * 获取当前 Instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * 获取当前 LocalDateTime
     */
    public static LocalDateTime nowLocal() {
        return LocalDateTime.now();
    }

    /**
     * 计算两个时间戳之间的毫秒差
     */
    public static long elapsedMillis(long startedNanos, long finishedNanos) {
        if (finishedNanos <= startedNanos) {
            return 0L;
        }
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(finishedNanos - startedNanos);
    }
}

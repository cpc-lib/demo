package cc.ivera.ragdemo.util;

import java.util.Collection;
import java.util.Map;

/**
 * 参数校验工具类
 * 统一项目中分散的参数校验逻辑，替代手写 null/blank 检查
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * 校验对象不为 null，否则抛出 IllegalArgumentException
     */
    public static <T> T requireNonNull(T obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return obj;
    }

    /**
     * 校验字符串不为空白，否则抛出 IllegalArgumentException
     */
    public static String requireNonBlank(String value, String name) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /**
     * 校验集合不为空，否则抛出 IllegalArgumentException
     */
    public static <T> Collection<T> requireNonEmpty(Collection<T> collection, String name) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return collection;
    }

    /**
     * 校验 Map 不为空，否则抛出 IllegalArgumentException
     */
    public static <K, V> Map<K, V> requireNonEmptyMap(Map<K, V> map, String name) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return map;
    }

    /**
     * 校验条件为 true，否则抛出 IllegalArgumentException
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验条件为 true，否则抛出 IllegalStateException
     */
    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 校验数值在范围内（含边界）
     */
    public static int requireInRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 校验数值不小于最小值
     */
    public static long requireMin(long value, long min, String name) {
        if (value < min) {
            throw new IllegalArgumentException(name + " must be at least " + min + ", but was " + value);
        }
        return value;
    }

    /**
     * 校验数值不大于最大值
     */
    public static long requireMax(long value, long max, String name) {
        if (value > max) {
            throw new IllegalArgumentException(name + " must be at most " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 校验字符串长度在范围内
     */
    public static String requireLength(String value, int min, int max, String name) {
        requireNonBlank(value, name);
        int len = value.length();
        if (len < min || len > max) {
            throw new IllegalArgumentException(
                    name + " length must be between " + min + " and " + max + ", but was " + len);
        }
        return value;
    }

    /**
     * 判断对象是否为 null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 判断对象是否不为 null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为空
     */
    public static boolean isEmptyMap(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为空
     */
    public static boolean isNotEmptyMap(Map<?, ?> map) {
        return !isEmptyMap(map);
    }
}

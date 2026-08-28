package cc.ivera.ragdemo.model.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public record PageResponse<T>(
        long pageNo,
        long pageSize,
        long total,
        long pages,
        long maxPageSize,
        String sortBy,
        String sortDirection,
        List<T> records
) {

    public static <T> PageResponse<T> of(long pageNo, long pageSize, long total, List<T> records) {
        long pages = pageSize <= 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        return new PageResponse<>(pageNo, pageSize, total, pages, 0, null, null, records);
    }

    public static <T> PageResponse<T> of(PageQuery query, long total, List<T> records) {
        long pageSize = query.effectivePageSize(safeTotal(total));
        long pages = pageSize <= 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        return new PageResponse<>(
                query.pageNo(),
                pageSize,
                total,
                pages,
                query.maxPageSize(),
                query.sortBy(),
                query.sortDirection(),
                records == null ? Collections.emptyList() : records
        );
    }

    public static <T> PageResponse<T> slice(List<T> source,
                                            Integer pageNo,
                                            Integer pageSize,
                                            Integer fallbackPageSize,
                                            int maxPageSize) {
        return slice(source, PageQuery.of(pageNo, pageSize, fallbackPageSize, maxPageSize));
    }

    public static <T> PageResponse<T> slice(List<T> source,
                                            PageQuery query) {
        List<T> rows = source == null ? Collections.emptyList() : source;
        PageQuery safeQuery = query == null
                ? PageQuery.of(null, null, null, PageQuery.DEFAULT_MAX_PAGE_SIZE)
                : query;
        List<T> sortedRows = sortRows(rows, safeQuery);
        int normalizedPageSize = safeQuery.effectivePageSize(sortedRows.size());
        int total = rows.size();
        int from = safeQuery.offset(total);
        int to = Math.min(from + normalizedPageSize, total);
        return of(safeQuery, total, sortedRows.subList(from, to));
    }

    private static <T> List<T> sortRows(List<T> rows, PageQuery query) {
        if (!query.sorted() || rows.size() < 2) {
            return rows;
        }
        List<T> sortedRows = new ArrayList<>(rows);
        Comparator<T> comparator = (left, right) -> compareNullableValues(
                sortableValue(left, query.sortBy()),
                sortableValue(right, query.sortBy()),
                query.ascending()
        );
        sortedRows.sort(comparator);
        return sortedRows;
    }

    private static Object sortableValue(Object row, String sortBy) {
        if (row == null || sortBy == null) {
            return null;
        }
        if (row instanceof Map<?, ?> map) {
            return map.get(sortBy);
        }
        Object value = invokeAccessor(row, sortBy);
        if (value != null) {
            return value;
        }
        return readField(row, sortBy);
    }

    private static Object invokeAccessor(Object row, String sortBy) {
        String suffix = Character.toUpperCase(sortBy.charAt(0)) + sortBy.substring(1);
        for (String methodName : List.of(sortBy, "get" + suffix, "is" + suffix)) {
            try {
                Method method = row.getClass().getDeclaredMethod(methodName);
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method.invoke(row);
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next conventional accessor shape.
            }
        }
        return null;
    }

    private static Object readField(Object row, String sortBy) {
        Class<?> type = row.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(sortBy);
                field.setAccessible(true);
                return field.get(row);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareValues(Object left, Object right) {
        if (left instanceof Comparable comparable && left.getClass().isInstance(right)) {
            return comparable.compareTo(right);
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static int compareNullableValues(Object left, Object right, boolean ascending) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int result = compareValues(left, right);
        return ascending ? result : -result;
    }

    private static int safeTotal(long total) {
        if (total <= 0) {
            return 0;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}

package cc.ivera.ragdemo.model.query;

import org.springframework.util.StringUtils;

import java.util.Locale;

public record PageQuery(
        int pageNo,
        Integer pageSize,
        int maxPageSize,
        String sortBy,
        String sortDirection
) {

    public static final int DEFAULT_MAX_PAGE_SIZE = 500;

    public static PageQuery of(Integer pageNo,
                               Integer pageSize,
                               Integer fallbackPageSize,
                               String sortBy,
                               String sortDirection,
                               int maxPageSize) {
        int safeMaxPageSize = maxPageSize <= 0 ? DEFAULT_MAX_PAGE_SIZE : maxPageSize;
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        Integer safePageSize = normalizePageSize(pageSize == null ? fallbackPageSize : pageSize, safeMaxPageSize);
        String safeSortBy = normalizeSortBy(sortBy);
        String safeSortDirection = safeSortBy == null ? null : normalizeSortDirection(sortDirection);
        return new PageQuery(safePageNo, safePageSize, safeMaxPageSize, safeSortBy, safeSortDirection);
    }

    public static PageQuery of(Integer pageNo, Integer pageSize, Integer fallbackPageSize, int maxPageSize) {
        return of(pageNo, pageSize, fallbackPageSize, null, null, maxPageSize);
    }

    public PageQuery withDefaultSort(String defaultSortBy, String defaultSortDirection) {
        if (sortBy != null) {
            return this;
        }
        return of(pageNo, pageSize, pageSize, defaultSortBy, defaultSortDirection, maxPageSize);
    }

    public int effectivePageSize(long total) {
        if (pageSize != null) {
            return pageSize;
        }
        return Math.min(Math.max(safeTotal(total), 1), maxPageSize);
    }

    public int offset(long total) {
        return Math.min((pageNo - 1) * effectivePageSize(total), Math.max(safeTotal(total), 0));
    }

    public boolean ascending() {
        return "ASC".equals(sortDirection);
    }

    public boolean sorted() {
        return sortBy != null;
    }

    private static Integer normalizePageSize(Integer value, int maxPageSize) {
        if (value == null) {
            return null;
        }
        if (value < 1) {
            return 20;
        }
        return Math.min(value, maxPageSize);
    }

    private static String normalizeSortBy(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.matches("[A-Za-z][A-Za-z0-9_.]*") ? trimmed : null;
    }

    private static String normalizeSortDirection(String value) {
        if (!StringUtils.hasText(value)) {
            return "DESC";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return "ASC".equals(normalized) ? "ASC" : "DESC";
    }

    private static int safeTotal(long total) {
        if (total <= 0) {
            return 0;
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }
}

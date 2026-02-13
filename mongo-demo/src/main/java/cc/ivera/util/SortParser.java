package cc.ivera.util;

import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.*;

public final class SortParser {

    private SortParser() {}

    /**
     * sort format:
     *  field,asc|desc;field2,asc|desc
     */
    public static Sort parse(String sortParam, Set<String> allowedFields, Sort defaultSort) {
        if (!StringUtils.hasText(sortParam)) {
            return defaultSort;
        }
        List<Sort.Order> orders = new ArrayList<>();
        String[] parts = sortParam.split(";");
        for (String part : parts) {
            if (!StringUtils.hasText(part)) continue;
            String[] kv = part.split(",");
            String field = kv[0].trim();
            if (!allowedFields.contains(field)) {
                // ignore illegal field (security hardening)
                continue;
            }
            Sort.Direction dir = Sort.Direction.DESC;
            if (kv.length >= 2) {
                try {
                    dir = Sort.Direction.fromString(kv[1].trim());
                } catch (Exception ignored) {}
            }
            orders.add(new Sort.Order(dir, field));
        }
        if (orders.isEmpty()) return defaultSort;
        return Sort.by(orders);
    }
}

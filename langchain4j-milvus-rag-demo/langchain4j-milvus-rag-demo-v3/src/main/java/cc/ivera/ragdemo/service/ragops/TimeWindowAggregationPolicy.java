package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TimeWindowAggregationPolicy {

    public enum Window {
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH
    }

    public Window normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return Window.DAY;
        }
        return switch (value.trim().toLowerCase()) {
            case "minute", "min" -> Window.MINUTE;
            case "hour", "h" -> Window.HOUR;
            case "week", "w" -> Window.WEEK;
            case "month", "m" -> Window.MONTH;
            default -> Window.DAY;
        };
    }

    public LocalDateTime bucket(LocalDateTime timestamp, String window) {
        return bucket(timestamp, normalize(window));
    }

    public LocalDateTime bucket(LocalDateTime timestamp, Window window) {
        LocalDateTime value = timestamp == null ? LocalDateTime.now() : timestamp;
        return switch (window) {
            case MINUTE -> value.truncatedTo(ChronoUnit.MINUTES);
            case HOUR -> value.truncatedTo(ChronoUnit.HOURS);
            case DAY -> value.toLocalDate().atStartOfDay();
            case WEEK -> value.toLocalDate()
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay();
            case MONTH -> value.toLocalDate()
                    .withDayOfMonth(1)
                    .atStartOfDay();
        };
    }

    public double percentile(List<Long> values, double percentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        List<Long> sorted = new ArrayList<>(values.stream()
                .filter(value -> value != null && value >= 0)
                .toList());
        if (sorted.isEmpty()) {
            return 0.0;
        }
        sorted.sort(Comparator.naturalOrder());
        double bounded = Math.max(0.0, Math.min(1.0, percentile));
        int index = (int) Math.ceil(bounded * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}

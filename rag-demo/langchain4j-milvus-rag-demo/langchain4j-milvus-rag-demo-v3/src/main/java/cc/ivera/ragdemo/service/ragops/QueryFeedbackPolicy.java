package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class QueryFeedbackPolicy {

    public static final String RATING_HELPFUL = "HELPFUL";
    public static final String RATING_NOT_HELPFUL = "NOT_HELPFUL";
    public static final String RATING_CORRECTION = "CORRECTION";

    private static final Set<String> SUPPORTED_RATINGS = Set.of(
            RATING_HELPFUL,
            RATING_NOT_HELPFUL,
            RATING_CORRECTION
    );

    public String normalizeRating(String rating) {
        if (rating == null || rating.isBlank()) {
            throw new IllegalArgumentException("rating must not be blank");
        }
        String normalized = rating.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RATINGS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported feedback rating: " + rating);
        }
        return normalized;
    }

    public String cleanText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (maxLength <= 0) {
            return "";
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}

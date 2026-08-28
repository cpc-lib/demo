package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RetrievalModePolicy {

    private static final int DEFAULT_TOP_K = 8;
    private static final int MAX_TOP_K = 50;

    public enum Mode {
        VECTOR,
        KEYWORD,
        HYBRID
    }

    public Mode normalize(String retrievalMode) {
        if (retrievalMode == null || retrievalMode.isBlank()) {
            return Mode.VECTOR;
        }
        String normalized = retrievalMode.trim().toUpperCase(Locale.ROOT);
        try {
            return Mode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported retrievalMode: " + retrievalMode);
        }
    }

    public int safeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(MAX_TOP_K, topK));
    }
}

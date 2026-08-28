package cc.ivera.ragdemo.service.ragops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class RetrievalFailureAnalysisPolicy {

    @Autowired
    public RetrievalFailureAnalysisPolicy() {
    }

    public FailureAnalysis analyze(String retrievalMode,
                                   List<String> expectedChunkIds,
                                   List<String> retrievedChunkIds,
                                   String queryCategory,
                                   int firstHitRank,
                                   double recall) {
        if (firstHitRank > 0 && recall >= 0.999) {
            return FailureAnalysis.none();
        }
        String mode = normalizeMode(retrievalMode);
        String failureType = failureType(mode, firstHitRank, recall);
        String missPattern = missPattern(expectedChunkIds, retrievedChunkIds);
        String category = StringUtils.hasText(queryCategory) ? queryCategory : "unknown";
        String clusterKey = "%s:%s:%s:%s".formatted(failureType, category, mode, missPattern);
        return new FailureAnalysis(
                failureType,
                failureReason(failureType, firstHitRank, recall),
                clusterKey,
                suggestion(failureType)
        );
    }

    public String suggestion(String failureType) {
        if (!StringUtils.hasText(failureType)) {
            return "";
        }
        return switch (failureType) {
            case "KEYWORD_MISS" -> "Check analyzer profile, synonyms, and business vocabulary coverage.";
            case "VECTOR_MISS" -> "Check chunking, embedding model, topK, and minScore configuration.";
            case "RERANK_DROP" -> "Check rerank candidate limit and whether relevant chunks were demoted.";
            case "FILTER_MISMATCH" -> "Check knowledge base, permission, content type, and current-version filters.";
            case "LOW_RANK" -> "Relevant chunks were retrieved but ranked too low; tune fusion weights or rerank.";
            case "LOW_RECALL" -> "Some expected chunks were retrieved, but recall is incomplete.";
            default -> "No expected chunks were retrieved; compare vector and keyword recall traces.";
        };
    }

    private String failureType(String mode, int firstHitRank, double recall) {
        if (recall <= 0.0) {
            return switch (mode) {
                case "keyword" -> "KEYWORD_MISS";
                case "vector" -> "VECTOR_MISS";
                default -> "MISS_ALL";
            };
        }
        if (firstHitRank > 3) {
            return "LOW_RANK";
        }
        if (recall < 0.999) {
            return "LOW_RECALL";
        }
        return "MISS_ALL";
    }

    private String failureReason(String failureType, int firstHitRank, double recall) {
        if ("LOW_RANK".equals(failureType)) {
            return "First expected chunk appeared at rank " + firstHitRank + ".";
        }
        if ("LOW_RECALL".equals(failureType)) {
            return "Only " + String.format(Locale.ROOT, "%.2f", recall) + " of expected chunks were retrieved.";
        }
        return "No expected chunk was retrieved in the evaluated topK.";
    }

    private String missPattern(List<String> expectedChunkIds, List<String> retrievedChunkIds) {
        Set<String> retrieved = new LinkedHashSet<>(retrievedChunkIds == null ? List.of() : retrievedChunkIds);
        List<String> missing = (expectedChunkIds == null ? List.<String>of() : expectedChunkIds).stream()
                .filter(StringUtils::hasText)
                .filter(chunkId -> !retrieved.contains(chunkId))
                .sorted()
                .toList();
        if (missing.isEmpty()) {
            return "partial";
        }
        return RagHashing.sha256Hex(String.join("|", missing)).substring(0, 12);
    }

    private String normalizeMode(String retrievalMode) {
        if (!StringUtils.hasText(retrievalMode)) {
            return "hybrid";
        }
        return retrievalMode.trim().toLowerCase(Locale.ROOT);
    }

    public record FailureAnalysis(
            String failureType,
            String failureReason,
            String clusterKey,
            String suggestion
    ) {
        public static FailureAnalysis none() {
            return new FailureAnalysis(null, null, null, null);
        }
    }
}

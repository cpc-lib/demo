package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.ragops.RetrievalFusionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ReciprocalRankFusionReranker implements RagReranker {

    private final RagProperties ragProperties;
    private final RetrievalFusionPolicy fusionPolicy;
    private ExternalRerankClient externalRerankClient;

    @Autowired(required = false)
    void setExternalRerankClient(ExternalRerankClient externalRerankClient) {
        this.externalRerankClient = externalRerankClient;
    }

    @Override
    public List<RagSearchItem> rerank(RagRetrievalCriteria criteria,
                                      List<RagRetrievalResultSet> resultSets,
                                      int topK) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        double rrfK = fusionPolicy.rrfK(ragProperties.getRetrieval());
        for (RagRetrievalResultSet resultSet : resultSets) {
            if (resultSet.items() == null || resultSet.items().isEmpty()) {
                continue;
            }
            double weight = resultSet.weight() <= 0 ? 1.0 : resultSet.weight();
            String source = resultSet.source();
            for (int i = 0; i < resultSet.items().size(); i++) {
                RagSearchItem item = resultSet.items().get(i);
                String key = uniqueKey(item);
                Candidate candidate = candidates.computeIfAbsent(key, ignored -> new Candidate(item));
                int rank = item.rank() > 0 ? item.rank() : i + 1;
                candidate.score += weight / (rrfK + rank);
                candidate.sources.add(source);
            }
        }

        int candidateLimit = candidateLimit(topK, candidates.size());
        List<Candidate> ranked = candidates.values().stream()
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(candidateLimit)
                .toList();

        List<RagSearchItem> items = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Candidate candidate = ranked.get(i);
            items.add(withHybridMetadata(candidate.item, i + 1, candidate.score, candidate.sources));
        }
        if (externalRerankClient != null && externalRerankClient.enabled()) {
            return externalRerankClient.rerank(criteria, items, topK);
        }
        return items.stream().limit(Math.max(1, topK)).toList();
    }

    private int candidateLimit(int topK, int total) {
        int safeTopK = Math.max(1, topK);
        int configured = ragProperties.getRetrieval().getRerankCandidateLimit();
        int limit = configured <= 0 ? safeTopK : Math.max(safeTopK, configured);
        return Math.min(Math.max(1, total), limit);
    }

    private RagSearchItem withHybridMetadata(RagSearchItem item,
                                             int rank,
                                             double score,
                                             Set<String> sources) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (item.metadata() != null) {
            metadata.putAll(item.metadata());
        }
        metadata.put("retrieval_mode", "hybrid");
        metadata.put("hybrid_sources", List.copyOf(sources));
        metadata.put("hybrid_score", score);
        return new RagSearchItem(
                rank,
                score,
                item.knowledgeBaseId(),
                item.documentId(),
                item.documentName(),
                item.chunkId(),
                item.version(),
                item.contentType(),
                item.pageNo(),
                item.sectionTitle(),
                item.imageCaption(),
                item.imageNumber(),
                item.imageUrl(),
                item.content(),
                metadata,
                item.modality(),
                String.join("+", sources),
                item.imageAssetId(),
                score
        );
    }

    private String uniqueKey(RagSearchItem item) {
        if (item.chunkId() != null && !item.chunkId().isBlank()) {
            return "chunk:" + item.chunkId();
        }
        return "doc:" + item.documentId() + ":content:" + item.content();
    }

    private static class Candidate {
        private final RagSearchItem item;
        private final Set<String> sources = new LinkedHashSet<>();
        private double score;

        private Candidate(RagSearchItem item) {
            this.item = item;
        }

        private double score() {
            return score;
        }
    }
}

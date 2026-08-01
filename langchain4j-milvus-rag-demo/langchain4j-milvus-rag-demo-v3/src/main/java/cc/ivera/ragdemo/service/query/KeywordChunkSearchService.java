package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.ragops.RetrievalModePolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KeywordChunkSearchService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final double BM25_K1 = 1.2;
    private static final double BM25_B = 0.75;

    private final RagDocumentChunkMapper chunkMapper;
    private final RetrievalFilterBuilder retrievalFilterBuilder;
    private final RetrievalModePolicy retrievalModePolicy;
    private final RagProperties ragProperties;
    private KeywordSearchIndex keywordSearchIndex;

    @Autowired(required = false)
    public void setKeywordSearchIndex(KeywordSearchIndex keywordSearchIndex) {
        this.keywordSearchIndex = keywordSearchIndex;
    }

    public List<RagSearchItem> search(RagRetrievalCriteria criteria) {
        String query = criteria.query() == null ? "" : criteria.query().trim();
        if (query.isBlank()) {
            return List.of();
        }

        KeywordQuery keywordQuery = tokenize(query);
        if (keywordQuery.searchTerms().isEmpty()) {
            return List.of();
        }

        int topK = retrievalModePolicy.safeTopK(criteria.topK());
        int candidateLimit = topK * Math.max(1, ragProperties.getRetrieval().getKeywordCandidateMultiplier());
        List<RagSearchItem> indexedItems = searchExternalIndex(criteria, candidateLimit, topK);
        if (!indexedItems.isEmpty()) {
            return indexedItems;
        }

        List<RagDocumentChunk> rows = chunkMapper.selectList(queryWrapper(criteria, keywordQuery, candidateLimit));
        List<RagDocumentChunk> permittedRows = new ArrayList<>();
        for (RagDocumentChunk row : rows) {
            Map<String, Object> metadata = metadata(row);
            if (!retrievalFilterBuilder.permissionsMatch(metadata, criteria.permissionTags())) {
                continue;
            }
            permittedRows.add(row);
        }

        Map<String, Double> scores = bm25Scores(permittedRows, keywordQuery.scoringTerms());
        List<RagSearchItem> items = new ArrayList<>();
        List<RagDocumentChunk> rankedRows = permittedRows.stream()
                .sorted(Comparator.comparingDouble((RagDocumentChunk row) -> scores.getOrDefault(row.getChunkUid(), 0.0)).reversed())
                .toList();
        for (RagDocumentChunk row : rankedRows) {
            Map<String, Object> metadata = metadata(row);
            metadata.put("keyword_algorithm", "bm25");
            metadata.put("keyword_terms", keywordQuery.scoringTerms());
            double score = scores.getOrDefault(row.getChunkUid(), 0.0);
            if (criteria.minScore() != null && score < criteria.minScore()) {
                continue;
            }
            items.add(toSearchItem(row, score, metadata, items.size() + 1));
            if (items.size() >= topK) {
                break;
            }
        }
        return items;
    }

    private List<RagSearchItem> searchExternalIndex(RagRetrievalCriteria criteria, int candidateLimit, int topK) {
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            return List.of();
        }
        List<RagSearchItem> indexed = keywordSearchIndex.search(criteria, candidateLimit);
        if (indexed.isEmpty()) {
            return List.of();
        }
        List<RagSearchItem> filtered = new ArrayList<>();
        for (RagSearchItem item : indexed) {
            if (!retrievalFilterBuilder.permissionsMatch(item.metadata(), criteria.permissionTags())) {
                continue;
            }
            if (criteria.minScore() != null && item.score() < criteria.minScore()) {
                continue;
            }
            filtered.add(withRank(item, filtered.size() + 1));
            if (filtered.size() >= topK) {
                break;
            }
        }
        return filtered;
    }

    private RagSearchItem withRank(RagSearchItem item, int rank) {
        return new RagSearchItem(
                rank,
                item.score(),
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
                item.metadata(),
                item.modality(),
                item.retrievalSource(),
                item.imageAssetId(),
                item.fusionScore()
        );
    }

    private LambdaQueryWrapper<RagDocumentChunk> queryWrapper(RagRetrievalCriteria criteria, KeywordQuery keywordQuery, int fetchLimit) {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(criteria.tenantId() == null ? 0L : criteria.tenantId());
        List<String> contentTypes = normalizedStrings(criteria.contentTypes());
        return new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getTenantId, tenantId)
                .in(criteria.knowledgeBaseIds() != null && !criteria.knowledgeBaseIds().isEmpty(),
                        RagDocumentChunk::getKnowledgeBaseId,
                        criteria.knowledgeBaseIds())
                .eq(RagDocumentChunk::getCurrentFlag, true)
                .eq(RagDocumentChunk::getChunkStatus, STATUS_ACTIVE)
                .eq(RagDocumentChunk::getIsDeleted, 0)
                .in(!contentTypes.isEmpty(), RagDocumentChunk::getContentType, contentTypes)
                .and(wrapper -> {
                    for (int i = 0; i < keywordQuery.searchTerms().size(); i++) {
                        String term = keywordQuery.searchTerms().get(i);
                        if (i > 0) {
                            wrapper.or();
                        }
                        wrapper.like(RagDocumentChunk::getContent, term)
                                .or()
                                .like(RagDocumentChunk::getContentSummary, term)
                                .or()
                                .like(RagDocumentChunk::getTitle, term)
                                .or()
                                .like(RagDocumentChunk::getSectionPath, term);
                    }
                })
                .orderByDesc(RagDocumentChunk::getUpdatedAt)
                .last("LIMIT " + Math.max(1, fetchLimit));
    }

    private RagSearchItem toSearchItem(RagDocumentChunk row, double score, Map<String, Object> metadata, int rank) {
        return new RagSearchItem(
                rank,
                score,
                row.getKnowledgeBaseId(),
                firstNonBlank(row.getSourceDocumentId(), row.getDocumentId() == null ? null : String.valueOf(row.getDocumentId())),
                row.getFileName(),
                row.getChunkUid(),
                row.getChunkVersion(),
                row.getContentType(),
                row.getPageStart(),
                firstNonBlank(row.getSectionPath(), row.getTitle()),
                row.getImageCaption(),
                row.getImageNumber(),
                row.getImageUrl(),
                row.getContent(),
                metadata,
                modalityFromContentType(row.getContentType()),
                "keyword",
                longValue(metadata.get("image_asset_id")),
                null
        );
    }

    private Map<String, Object> metadata(RagDocumentChunk row) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retrieval_mode", "keyword");
        metadata.put("tenant_id", stringValue(row.getTenantId()));
        metadata.put("knowledge_base_id", stringValue(row.getKnowledgeBaseId()));
        metadata.put("document_id", firstNonBlank(row.getSourceDocumentId(), stringValue(row.getDocumentId())));
        metadata.put("fileName", row.getFileName());
        metadata.put("chunk_id", row.getChunkUid());
        metadata.put("version", row.getChunkVersion());
        metadata.put("content_type", row.getContentType());
        metadata.put("page_no", row.getPageStart());
        metadata.put("section_title", firstNonBlank(row.getSectionPath(), row.getTitle()));
        metadata.put("image_caption", row.getImageCaption());
        metadata.put("image_number", row.getImageNumber());
        metadata.put("image_url", row.getImageUrl());
        metadata.put("permission_tags", row.getPermissionTags());
        metadata.put("current", String.valueOf(Boolean.TRUE.equals(row.getCurrentFlag())));
        metadata.put("chunk_status", row.getChunkStatus());
        metadata.put("vector_id", row.getVectorId());
        metadata.put("metadata_json", row.getMetadataJson());
        return metadata;
    }

    private String modalityFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "text";
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("image") || normalized.contains("chart") || normalized.contains("table")
                ? "image"
                : "text";
    }

    private Map<String, Double> bm25Scores(List<RagDocumentChunk> rows, List<String> terms) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (rows.isEmpty() || terms.isEmpty()) {
            return scores;
        }
        double avgLength = rows.stream().mapToInt(this::documentLength).average().orElse(1.0);
        Map<String, Integer> documentFrequency = new LinkedHashMap<>();
        for (String term : terms) {
            int df = 0;
            for (RagDocumentChunk row : rows) {
                if (termFrequency(row, term) > 0) {
                    df++;
                }
            }
            documentFrequency.put(term, df);
        }
        int total = rows.size();
        for (RagDocumentChunk row : rows) {
            double rawScore = 0.0;
            int length = Math.max(1, documentLength(row));
            for (String term : terms) {
                int df = documentFrequency.getOrDefault(term, 0);
                if (df == 0) {
                    continue;
                }
                double tf = termFrequency(row, term);
                if (tf <= 0) {
                    continue;
                }
                double idf = Math.log(1 + (total - df + 0.5) / (df + 0.5));
                double denominator = tf + BM25_K1 * (1 - BM25_B + BM25_B * length / Math.max(1.0, avgLength));
                rawScore += idf * (tf * (BM25_K1 + 1)) / denominator;
            }
            scores.put(row.getChunkUid(), normalizeBm25(rawScore));
        }
        return scores;
    }

    private double normalizeBm25(double rawScore) {
        if (rawScore <= 0) {
            return 0.0;
        }
        return Math.min(1.0, rawScore / (rawScore + 1.5));
    }

    private int documentLength(RagDocumentChunk row) {
        return Math.max(1,
                tokenizeText(row.getTitle()).size()
                        + tokenizeText(row.getSectionPath()).size()
                        + tokenizeText(row.getContentSummary()).size()
                        + tokenizeText(row.getContent()).size());
    }

    private double termFrequency(RagDocumentChunk row, String term) {
        return occurrences(row.getTitle(), term) * 2.0
                + occurrences(row.getSectionPath(), term) * 1.4
                + occurrences(row.getContentSummary(), term) * 1.2
                + occurrences(row.getContent(), term);
    }

    private int occurrences(String value, String term) {
        if (value == null || term.isBlank()) {
            return 0;
        }
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        int count = 0;
        int index = normalizedValue.indexOf(term);
        while (index >= 0) {
            count++;
            index = normalizedValue.indexOf(term, index + Math.max(1, term.length()));
        }
        return count;
    }

    private KeywordQuery tokenize(String query) {
        List<String> tokens = tokenizeText(query);
        LinkedHashSet<String> searchTerms = new LinkedHashSet<>();
        if (StringUtils.hasText(query)) {
            searchTerms.add(query.trim().toLowerCase(Locale.ROOT));
        }
        searchTerms.addAll(tokens);
        return new KeywordQuery(List.copyOf(searchTerms), tokens.isEmpty() ? List.copyOf(searchTerms) : tokens);
    }

    private List<String> tokenizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();
        StringBuilder ascii = new StringBuilder();
        List<Character> cjk = new ArrayList<>();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isLetterOrDigit(ch) && ch < 128) {
                ascii.append(ch);
                flushCjk(cjk, tokens);
            } else {
                flushAscii(ascii, tokens);
                if (isCjk(ch)) {
                    cjk.add(ch);
                } else {
                    flushCjk(cjk, tokens);
                }
            }
        }
        flushAscii(ascii, tokens);
        flushCjk(cjk, tokens);
        return List.copyOf(tokens);
    }

    private void flushAscii(StringBuilder ascii, Set<String> tokens) {
        if (ascii.length() > 0) {
            tokens.add(ascii.toString());
            ascii.setLength(0);
        }
    }

    private void flushCjk(List<Character> cjk, Set<String> tokens) {
        if (cjk.isEmpty()) {
            return;
        }
        if (cjk.size() == 1) {
            tokens.add(String.valueOf(cjk.get(0)));
        } else {
            for (int i = 0; i < cjk.size() - 1; i++) {
                tokens.add("" + cjk.get(i) + cjk.get(i + 1));
            }
        }
        cjk.clear();
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private List<String> normalizedStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record KeywordQuery(List<String> searchTerms, List<String> scoringTerms) {
    }
}

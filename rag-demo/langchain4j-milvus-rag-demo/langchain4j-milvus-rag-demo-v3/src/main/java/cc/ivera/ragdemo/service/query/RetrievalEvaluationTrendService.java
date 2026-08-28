package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCase;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCaseResult;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCluster;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalRun;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalCaseMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalCaseResultMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalClusterMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalRunMapper;
import cc.ivera.ragdemo.model.query.*;
import cc.ivera.ragdemo.service.ragops.RetrievalQueryClassificationPolicy;
import cc.ivera.ragdemo.service.ragops.TimeWindowAggregationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RetrievalEvaluationTrendService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 500;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final RagRetrievalEvalRunMapper runMapper;
    private final RagRetrievalEvalCaseResultMapper resultMapper;
    private final RagRetrievalEvalCaseMapper caseMapper;
    private final RagRetrievalEvalClusterMapper clusterMapper;
    private final TimeWindowAggregationPolicy timeWindowPolicy;
    private final RetrievalQueryClassificationPolicy classificationPolicy;
    private final ObjectMapper objectMapper;

    public List<RagRetrievalEvaluationTrendPoint> trends(Long tenantId,
                                                         Long knowledgeBaseId,
                                                         String versionTag,
                                                         String retrievalMode,
                                                         String queryCategory,
                                                         String language,
                                                         String difficultyLevel,
                                                         String window,
                                                         LocalDateTime from,
                                                         LocalDateTime to) {
        List<EvaluationRow> rows = rows(tenantId, knowledgeBaseId, versionTag, retrievalMode, from, to).stream()
                .filter(row -> matches(row.queryCategory(), queryCategory))
                .filter(row -> matches(row.language(), language))
                .filter(row -> matches(row.difficultyLevel(), difficultyLevel))
                .toList();
        Map<TrendKey, List<EvaluationRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> new TrendKey(
                        timeWindowPolicy.bucket(row.createdAt(), window),
                        timeWindowPolicy.normalize(window).name().toLowerCase(),
                        row.knowledgeBaseId(),
                        row.versionTag(),
                        row.retrievalMode(),
                        row.queryCategory(),
                        row.language(),
                        row.difficultyLevel(),
                        row.topK()
                ), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = metrics(entry.getValue());
                    TrendKey key = entry.getKey();
                    return new RagRetrievalEvaluationTrendPoint(
                            key.bucket(),
                            key.window(),
                            key.knowledgeBaseId(),
                            key.versionTag(),
                            key.retrievalMode(),
                            key.queryCategory(),
                            key.language(),
                            key.difficultyLevel(),
                            key.topK(),
                            metrics.total(),
                            metrics.hitRate(),
                            metrics.mrr(),
                            metrics.recall(),
                            metrics.failureRate(),
                            metrics.rerankDropRate(),
                            metrics.keywordOnlyHitRate(),
                            metrics.vectorOnlyHitRate()
                    );
                })
                .toList();
    }

    public List<RagRetrievalEvaluationSliceItem> slices(Long tenantId,
                                                       Long knowledgeBaseId,
                                                       String versionTag,
                                                       String retrievalMode,
                                                       String dimension,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        String dim = normalizeDimension(dimension);
        Function<EvaluationRow, String> extractor = dimensionExtractor(dim);
        Map<String, List<EvaluationRow>> grouped = rows(tenantId, knowledgeBaseId, versionTag, retrievalMode, from, to).stream()
                .collect(Collectors.groupingBy(row -> stringValue(extractor.apply(row)), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    Metrics metrics = metrics(entry.getValue());
                    return new RagRetrievalEvaluationSliceItem(
                            dim,
                            entry.getKey(),
                            metrics.total(),
                            metrics.hitRate(),
                            metrics.mrr(),
                            metrics.recall(),
                            metrics.failureRate()
                    );
                })
                .toList();
    }

    public List<RagRetrievalFailureClusterItem> failureClusters(Long runId,
                                                               Long tenantId,
                                                               Long knowledgeBaseId,
                                                               String retrievalMode) {
        if (runId != null) {
            List<RagRetrievalEvalCluster> persisted = clusterMapper.selectList(new LambdaQueryWrapper<RagRetrievalEvalCluster>()
                    .eq(RagRetrievalEvalCluster::getRunId, runId)
                    .orderByDesc(RagRetrievalEvalCluster::getCaseCount));
            if (!persisted.isEmpty()) {
                return persisted.stream()
                        .map(row -> new RagRetrievalFailureClusterItem(
                                row.getClusterKey(),
                                row.getClusterLabel(),
                                row.getFailureType(),
                                categoryFromCluster(row.getClusterKey()),
                                retrievalModeFromCluster(row.getClusterKey()),
                                row.getCaseCount() == null ? 0 : row.getCaseCount(),
                                readList(row.getSampleCaseIdsJson()),
                                row.getSuggestion()
                        ))
                        .toList();
            }
        }
        List<EvaluationRow> failures = rows(tenantId, knowledgeBaseId, null, retrievalMode, null, null).stream()
                .filter(row -> StringUtils.hasText(row.clusterKey()))
                .filter(row -> runId == null || Objects.equals(row.runId(), runId))
                .toList();
        Map<String, List<EvaluationRow>> grouped = failures.stream()
                .collect(Collectors.groupingBy(EvaluationRow::clusterKey, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<EvaluationRow> group = entry.getValue();
                    String failureType = group.stream().map(EvaluationRow::failureType).filter(StringUtils::hasText).findFirst().orElse("UNKNOWN");
                    return new RagRetrievalFailureClusterItem(
                            entry.getKey(),
                            clusterLabel(entry.getKey(), failureType),
                            failureType,
                            categoryFromCluster(entry.getKey()),
                            retrievalModeFromCluster(entry.getKey()),
                            group.size(),
                            group.stream().map(EvaluationRow::caseId).filter(StringUtils::hasText).limit(5).toList(),
                            suggestion(failureType)
                    );
                })
                .toList();
    }

    public PageResponse<RagRetrievalEvalCaseResult> failureClusterCases(String clusterKey,
                                                                        Long runId,
                                                                        Integer pageNo,
                                                                        Integer pageSize,
                                                                        String sortBy,
                                                                        String sortDirection) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        LambdaQueryWrapper<RagRetrievalEvalCaseResult> countQuery = clusterCaseQuery(clusterKey, runId);
        long total = resultMapper.selectCount(countQuery);
        LambdaQueryWrapper<RagRetrievalEvalCaseResult> rowsQuery = clusterCaseQuery(clusterKey, runId);
        boolean asc = pageQuery.ascending();
        if ("id".equals(pageQuery.sortBy())) {
            rowsQuery.orderBy(true, asc, RagRetrievalEvalCaseResult::getId);
        } else {
            rowsQuery.orderBy(true, asc, RagRetrievalEvalCaseResult::getCreatedAt);
        }
        rowsQuery.last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, resultMapper.selectList(rowsQuery));
    }

    public RagRetrievalEvaluationRunComparison compareRuns(Long leftRunId, Long rightRunId) {
        RagRetrievalEvalRun left = requiredRun(leftRunId);
        RagRetrievalEvalRun right = requiredRun(rightRunId);
        return new RagRetrievalEvaluationRunComparison(
                left.getId(),
                right.getId(),
                intValue(left.getTotalCases()),
                intValue(right.getTotalCases()),
                doubleValue(left.getHitRate()),
                doubleValue(right.getHitRate()),
                doubleValue(right.getHitRate()) - doubleValue(left.getHitRate()),
                doubleValue(left.getMeanReciprocalRank()),
                doubleValue(right.getMeanReciprocalRank()),
                doubleValue(right.getMeanReciprocalRank()) - doubleValue(left.getMeanReciprocalRank()),
                doubleValue(left.getMeanRecall()),
                doubleValue(right.getMeanRecall()),
                doubleValue(right.getMeanRecall()) - doubleValue(left.getMeanRecall())
        );
    }

    private List<EvaluationRow> rows(Long tenantId,
                                     Long knowledgeBaseId,
                                     String versionTag,
                                     String retrievalMode,
                                     LocalDateTime from,
                                     LocalDateTime to) {
        List<RagRetrievalEvalRun> runs = runMapper.selectList(runQuery(tenantId, knowledgeBaseId, versionTag, retrievalMode, from, to)
                .orderByAsc(RagRetrievalEvalRun::getCreatedAt));
        if (runs.isEmpty()) {
            return List.of();
        }
        List<Long> runIds = runs.stream().map(RagRetrievalEvalRun::getId).filter(Objects::nonNull).toList();
        Map<Long, RagRetrievalEvalRun> runsById = runs.stream()
                .collect(Collectors.toMap(RagRetrievalEvalRun::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<RagRetrievalEvalCaseResult> results = resultMapper.selectList(new LambdaQueryWrapper<RagRetrievalEvalCaseResult>()
                .in(RagRetrievalEvalCaseResult::getRunId, runIds)
                .orderByAsc(RagRetrievalEvalCaseResult::getId));
        Map<Long, RagRetrievalEvalCase> casesById = loadCases(results);
        List<EvaluationRow> rows = new ArrayList<>();
        for (RagRetrievalEvalCaseResult result : results) {
            RagRetrievalEvalRun run = runsById.get(result.getRunId());
            if (run == null) {
                continue;
            }
            RagRetrievalEvalCase evalCase = result.getCaseDbId() == null ? null : casesById.get(result.getCaseDbId());
            String query = evalCase == null ? result.getQueryText() : evalCase.getQueryText();
            rows.add(new EvaluationRow(
                    run.getId(),
                    result.getCaseId(),
                    run.getKnowledgeBaseId(),
                    run.getVersionTag(),
                    run.getRetrievalMode(),
                    result.getTopK(),
                    classificationPolicy.category(query, evalCase == null ? null : evalCase.getQueryCategory()),
                    classificationPolicy.language(query, evalCase == null ? null : evalCase.getLanguage()),
                    classificationPolicy.difficulty(query, evalCase == null ? null : evalCase.getDifficultyLevel()),
                    Boolean.TRUE.equals(result.getHit()),
                    doubleValue(result.getReciprocalRank()),
                    doubleValue(result.getRecall()),
                    result.getFailureType(),
                    result.getClusterKey(),
                    run.getCreatedAt()
            ));
        }
        return rows;
    }

    private Map<Long, RagRetrievalEvalCase> loadCases(List<RagRetrievalEvalCaseResult> results) {
        List<Long> caseIds = results.stream()
                .map(RagRetrievalEvalCaseResult::getCaseDbId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (caseIds.isEmpty()) {
            return Map.of();
        }
        return caseMapper.selectList(new LambdaQueryWrapper<RagRetrievalEvalCase>()
                        .in(RagRetrievalEvalCase::getId, caseIds))
                .stream()
                .collect(Collectors.toMap(RagRetrievalEvalCase::getId, Function.identity(), (left, right) -> left));
    }

    private LambdaQueryWrapper<RagRetrievalEvalRun> runQuery(Long tenantId,
                                                            Long knowledgeBaseId,
                                                            String versionTag,
                                                            String retrievalMode,
                                                            LocalDateTime from,
                                                            LocalDateTime to) {
        return new LambdaQueryWrapper<RagRetrievalEvalRun>()
                .eq(tenantId != null, RagRetrievalEvalRun::getTenantId, tenantId)
                .eq(knowledgeBaseId != null, RagRetrievalEvalRun::getKnowledgeBaseId, knowledgeBaseId)
                .eq(StringUtils.hasText(versionTag), RagRetrievalEvalRun::getVersionTag, versionTag)
                .eq(StringUtils.hasText(retrievalMode), RagRetrievalEvalRun::getRetrievalMode, retrievalMode)
                .ge(from != null, RagRetrievalEvalRun::getCreatedAt, from)
                .le(to != null, RagRetrievalEvalRun::getCreatedAt, to);
    }

    private LambdaQueryWrapper<RagRetrievalEvalCaseResult> clusterCaseQuery(String clusterKey, Long runId) {
        return new LambdaQueryWrapper<RagRetrievalEvalCaseResult>()
                .eq(StringUtils.hasText(clusterKey), RagRetrievalEvalCaseResult::getClusterKey, clusterKey)
                .eq(runId != null, RagRetrievalEvalCaseResult::getRunId, runId);
    }

    private Metrics metrics(List<EvaluationRow> rows) {
        if (rows.isEmpty()) {
            return new Metrics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        long total = rows.size();
        long hits = rows.stream().filter(EvaluationRow::hit).count();
        long failures = total - hits;
        long rerankDrops = rows.stream().filter(row -> "RERANK_DROP".equals(row.failureType())).count();
        long keywordOnlyHits = rows.stream().filter(row -> row.hit() && "keyword".equalsIgnoreCase(row.retrievalMode())).count();
        long vectorOnlyHits = rows.stream().filter(row -> row.hit() && "vector".equalsIgnoreCase(row.retrievalMode())).count();
        return new Metrics(
                total,
                hits / (double) total,
                rows.stream().mapToDouble(EvaluationRow::reciprocalRank).average().orElse(0.0),
                rows.stream().mapToDouble(EvaluationRow::recall).average().orElse(0.0),
                failures / (double) total,
                rerankDrops / (double) total,
                keywordOnlyHits / (double) total,
                vectorOnlyHits / (double) total
        );
    }

    private String normalizeDimension(String dimension) {
        if (!StringUtils.hasText(dimension)) {
            return "queryCategory";
        }
        return switch (dimension.trim()) {
            case "language", "difficultyLevel", "retrievalMode", "versionTag", "topK" -> dimension.trim();
            default -> "queryCategory";
        };
    }

    private Function<EvaluationRow, String> dimensionExtractor(String dimension) {
        return switch (dimension) {
            case "language" -> EvaluationRow::language;
            case "difficultyLevel" -> EvaluationRow::difficultyLevel;
            case "retrievalMode" -> EvaluationRow::retrievalMode;
            case "versionTag" -> EvaluationRow::versionTag;
            case "topK" -> row -> stringValue(row.topK());
            default -> EvaluationRow::queryCategory;
        };
    }

    private boolean matches(String actual, String expected) {
        return !StringUtils.hasText(expected) || expected.equalsIgnoreCase(actual);
    }

    private RagRetrievalEvalRun requiredRun(Long runId) {
        if (runId == null) {
            throw new IllegalArgumentException("runId is required");
        }
        RagRetrievalEvalRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new IllegalArgumentException("Evaluation run not found: " + runId);
        }
        return run;
    }

    private List<String> readList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String categoryFromCluster(String clusterKey) {
        String[] parts = splitCluster(clusterKey);
        return parts.length > 1 ? parts[1] : "unknown";
    }

    private String retrievalModeFromCluster(String clusterKey) {
        String[] parts = splitCluster(clusterKey);
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private String clusterLabel(String clusterKey, String failureType) {
        String[] parts = splitCluster(clusterKey);
        if (parts.length > 2) {
            return parts[0] + " / " + parts[1] + " / " + parts[2];
        }
        return failureType;
    }

    private String suggestion(String failureType) {
        return switch (failureType) {
            case "KEYWORD_MISS" -> "Check analyzer profile, synonyms, and business vocabulary coverage.";
            case "VECTOR_MISS" -> "Check chunking, embedding model, topK, and minScore configuration.";
            case "LOW_RANK" -> "Relevant chunks were retrieved but ranked too low; tune fusion weights or rerank.";
            case "LOW_RECALL" -> "Some expected chunks were retrieved, but recall is incomplete.";
            default -> "Inspect retrieval filters, vector recall, keyword recall, and rerank inputs.";
        };
    }

    private String[] splitCluster(String clusterKey) {
        return StringUtils.hasText(clusterKey) ? clusterKey.split(":") : new String[0];
    }

    private String stringValue(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private double doubleValue(Double value) {
        return value == null ? 0.0 : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private record TrendKey(
            LocalDateTime bucket,
            String window,
            Long knowledgeBaseId,
            String versionTag,
            String retrievalMode,
            String queryCategory,
            String language,
            String difficultyLevel,
            Integer topK
    ) {
    }

    private record EvaluationRow(
            Long runId,
            String caseId,
            Long knowledgeBaseId,
            String versionTag,
            String retrievalMode,
            Integer topK,
            String queryCategory,
            String language,
            String difficultyLevel,
            boolean hit,
            double reciprocalRank,
            double recall,
            String failureType,
            String clusterKey,
            LocalDateTime createdAt
    ) {
    }

    private record Metrics(
            long total,
            double hitRate,
            double mrr,
            double recall,
            double failureRate,
            double rerankDropRate,
            double keywordOnlyHitRate,
            double vectorOnlyHitRate
    ) {
    }
}

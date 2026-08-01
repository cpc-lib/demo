package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCase;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCaseResult;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCluster;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalRun;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalCaseMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalCaseResultMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalClusterMapper;
import cc.ivera.ragdemo.mapper.RagRetrievalEvalRunMapper;
import cc.ivera.ragdemo.model.query.*;
import cc.ivera.ragdemo.service.ragops.RetrievalFailureAnalysisPolicy;
import cc.ivera.ragdemo.service.ragops.RetrievalQueryClassificationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagRetrievalEvaluationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 500;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final RagRetrievalService retrievalService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final RagRetrievalEvalCaseMapper caseMapper;
    private final RagRetrievalEvalRunMapper runMapper;
    private final RagRetrievalEvalCaseResultMapper resultMapper;
    private final RagRetrievalEvalClusterMapper clusterMapper;
    private final RetrievalQueryClassificationPolicy queryClassificationPolicy;
    private final RetrievalFailureAnalysisPolicy failureAnalysisPolicy;

    public RagRetrievalEvaluationResponse evaluate(List<RagRetrievalEvaluationCase> cases) {
        return evaluate(new RagRetrievalEvaluationRequest(null, null, null, null, null, cases));
    }

    public RagRetrievalEvaluationResponse evaluate(RagRetrievalEvaluationRequest request) {
        CaseSource source = resolveCases(request);
        List<RagRetrievalEvaluationCaseResult> results = source.cases().stream()
                .filter(item -> StringUtils.hasText(item.query()))
                .map(this::evaluateCase)
                .toList();
        Metrics metrics = metrics(results);
        RagRetrievalEvalRun run = persistRun(request, source, metrics);
        for (RagRetrievalEvaluationCaseResult result : results) {
            persistResult(run.getId(), source.caseDbIdsByCaseId().get(result.caseId()), result);
        }
        persistFailureClusters(run.getId(), results);
        return new RagRetrievalEvaluationResponse(
                run.getId(),
                run.getRunNo(),
                run.getKnowledgeBaseId(),
                run.getVersionTag(),
                run.getRetrievalMode(),
                results.size(),
                metrics.hitRate(),
                metrics.meanReciprocalRank(),
                metrics.meanRecall(),
                results
        );
    }

    public PageResponse<RagRetrievalEvalCase> pageCases(Long tenantId,
                                                        Long knowledgeBaseId,
                                                        String versionTag,
                                                        Boolean enabled,
                                                        Integer pageNo,
                                                        Integer pageSize,
                                                        String sortBy,
                                                        String sortDirection) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("updatedAt", "DESC");
        LambdaQueryWrapper<RagRetrievalEvalCase> countQuery = caseQuery(tenantId, knowledgeBaseId, versionTag, enabled);
        long total = caseMapper.selectCount(countQuery);
        LambdaQueryWrapper<RagRetrievalEvalCase> rowsQuery = caseQuery(tenantId, knowledgeBaseId, versionTag, enabled);
        applyCaseOrder(rowsQuery, pageQuery);
        rowsQuery.last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, caseMapper.selectList(rowsQuery));
    }

    public RagRetrievalEvalCase saveCase(RagRetrievalEvaluationCaseUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            throw new IllegalArgumentException("Evaluation query is required");
        }
        if (request.expectedChunkIds() == null || request.expectedChunkIds().isEmpty()) {
            throw new IllegalArgumentException("Expected chunk ids are required");
        }
        RagRetrievalEvalCase row = request.id() == null ? new RagRetrievalEvalCase() : caseMapper.selectById(request.id());
        if (row == null) {
            throw new IllegalArgumentException("Evaluation case not found: " + request.id());
        }
        LocalDateTime now = LocalDateTime.now();
        row.setTenantId(request.tenantId() == null ? 0L : request.tenantId());
        row.setKnowledgeBaseId(request.knowledgeBaseId());
        row.setVersionTag(defaultVersionTag(request.versionTag()));
        row.setCaseId(StringUtils.hasText(request.caseId())
                ? request.caseId().trim()
                : "case_" + UUID.randomUUID().toString().replace("-", ""));
        row.setQueryText(request.query().trim());
        row.setRetrievalMode(defaultRetrievalMode(request.retrievalMode()));
        row.setQueryCategory(queryClassificationPolicy.category(request.query(), request.queryCategory()));
        row.setDifficultyLevel(queryClassificationPolicy.difficulty(request.query(), request.difficultyLevel()));
        row.setLanguage(queryClassificationPolicy.language(request.query(), request.language()));
        row.setExpectedAnswerType(defaultExpectedAnswerType(request.expectedAnswerType()));
        row.setTopK(request.topK());
        row.setMinScore(request.minScore());
        row.setContentTypesJson(writeList(request.contentTypes()));
        row.setPermissionTagsJson(writeList(request.permissionTags()));
        row.setExpectedChunkIdsJson(writeList(request.expectedChunkIds()));
        row.setEnabled(request.enabled() == null || request.enabled());
        row.setMetadataJson(request.metadataJson());
        row.setUpdatedAt(now);
        row.setIsDeleted(0);
        if (row.getId() == null) {
            row.setCreatedAt(now);
            caseMapper.insert(row);
        } else {
            caseMapper.updateById(row);
        }
        return row;
    }

    public void deleteCase(Long id) {
        if (id == null) {
            return;
        }
        caseMapper.update(null, new LambdaUpdateWrapper<RagRetrievalEvalCase>()
                .eq(RagRetrievalEvalCase::getId, id)
                .set(RagRetrievalEvalCase::getIsDeleted, 1)
                .set(RagRetrievalEvalCase::getUpdatedAt, LocalDateTime.now()));
    }

    public PageResponse<RagRetrievalEvalRun> pageRuns(Long tenantId,
                                                      Long knowledgeBaseId,
                                                      String versionTag,
                                                      String retrievalMode,
                                                      Integer pageNo,
                                                      Integer pageSize,
                                                      String sortBy,
                                                      String sortDirection) {
        PageQuery pageQuery = PageQuery.of(pageNo, pageSize, DEFAULT_PAGE_SIZE, sortBy, sortDirection, MAX_PAGE_SIZE)
                .withDefaultSort("createdAt", "DESC");
        LambdaQueryWrapper<RagRetrievalEvalRun> countQuery = runQuery(tenantId, knowledgeBaseId, versionTag, retrievalMode);
        long total = runMapper.selectCount(countQuery);
        LambdaQueryWrapper<RagRetrievalEvalRun> rowsQuery = runQuery(tenantId, knowledgeBaseId, versionTag, retrievalMode);
        applyRunOrder(rowsQuery, pageQuery);
        rowsQuery.last("LIMIT " + pageQuery.offset(total) + ", " + pageQuery.effectivePageSize(total));
        return PageResponse.of(pageQuery, total, runMapper.selectList(rowsQuery));
    }

    public RagRetrievalEvaluationRunDetailResponse runDetail(Long runId) {
        RagRetrievalEvalRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new IllegalArgumentException("Evaluation run not found: " + runId);
        }
        List<RagRetrievalEvalCaseResult> results = resultMapper.selectList(new LambdaQueryWrapper<RagRetrievalEvalCaseResult>()
                .eq(RagRetrievalEvalCaseResult::getRunId, runId)
                .orderByAsc(RagRetrievalEvalCaseResult::getId));
        return new RagRetrievalEvaluationRunDetailResponse(run, results);
    }

    public List<RagRetrievalEvaluationReportItem> report(Long tenantId,
                                                         Long knowledgeBaseId,
                                                         String retrievalMode) {
        List<RagRetrievalEvalRun> runs = runMapper.selectList(runQuery(tenantId, knowledgeBaseId, null, retrievalMode)
                .orderByDesc(RagRetrievalEvalRun::getCreatedAt));
        Map<String, List<RagRetrievalEvalRun>> grouped = runs.stream()
                .collect(Collectors.groupingBy(this::reportKey, LinkedHashMap::new, Collectors.toList()));
        List<RagRetrievalEvaluationReportItem> items = new ArrayList<>();
        for (List<RagRetrievalEvalRun> group : grouped.values()) {
            RagRetrievalEvalRun latest = group.get(0);
            long totalCases = group.stream()
                    .map(RagRetrievalEvalRun::getTotalCases)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();
            items.add(new RagRetrievalEvaluationReportItem(
                    latest.getKnowledgeBaseId(),
                    latest.getVersionTag(),
                    latest.getRetrievalMode(),
                    group.size(),
                    totalCases,
                    group.stream().map(RagRetrievalEvalRun::getHitRate).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0),
                    group.stream().map(RagRetrievalEvalRun::getMeanReciprocalRank).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0),
                    group.stream().map(RagRetrievalEvalRun::getMeanRecall).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0),
                    latest.getId(),
                    latest.getRunNo(),
                    latest.getCreatedAt()
            ));
        }
        return items;
    }

    private CaseSource resolveCases(RagRetrievalEvaluationRequest request) {
        if (request != null && request.cases() != null && !request.cases().isEmpty()) {
            return new CaseSource(request.cases(), Map.of(), "request");
        }
        List<RagRetrievalEvalCase> stored = storedCases(request);
        if (!stored.isEmpty()) {
            Map<String, Long> ids = stored.stream()
                    .collect(Collectors.toMap(RagRetrievalEvalCase::getCaseId, RagRetrievalEvalCase::getId, (left, right) -> left));
            return new CaseSource(stored.stream().map(this::toCase).toList(), ids, "mysql");
        }
        return new CaseSource(loadDefaultCases(), Map.of(), "file");
    }

    private List<RagRetrievalEvalCase> storedCases(RagRetrievalEvaluationRequest request) {
        LambdaQueryWrapper<RagRetrievalEvalCase> query = new LambdaQueryWrapper<RagRetrievalEvalCase>()
                .eq(RagRetrievalEvalCase::getIsDeleted, 0)
                .eq(RagRetrievalEvalCase::getEnabled, true);
        if (request != null) {
            query.eq(request.tenantId() != null, RagRetrievalEvalCase::getTenantId, request.tenantId())
                    .eq(request.knowledgeBaseId() != null, RagRetrievalEvalCase::getKnowledgeBaseId, request.knowledgeBaseId())
                    .eq(StringUtils.hasText(request.versionTag()), RagRetrievalEvalCase::getVersionTag, request.versionTag())
                    .eq(StringUtils.hasText(request.retrievalMode()), RagRetrievalEvalCase::getRetrievalMode, request.retrievalMode())
                    .in(request.caseIds() != null && !request.caseIds().isEmpty(), RagRetrievalEvalCase::getId, request.caseIds());
        }
        return caseMapper.selectList(query.orderByAsc(RagRetrievalEvalCase::getId));
    }

    private RagRetrievalEvaluationCase toCase(RagRetrievalEvalCase row) {
        return new RagRetrievalEvaluationCase(
                row.getCaseId(),
                row.getQueryText(),
                row.getTenantId(),
                row.getKnowledgeBaseId() == null ? List.of() : List.of(row.getKnowledgeBaseId()),
                row.getRetrievalMode(),
                row.getTopK(),
                row.getMinScore(),
                readList(row.getContentTypesJson()),
                readList(row.getPermissionTagsJson()),
                readList(row.getExpectedChunkIdsJson()),
                row.getId(),
                row.getKnowledgeBaseId(),
                row.getVersionTag(),
                row.getQueryCategory(),
                row.getDifficultyLevel(),
                row.getLanguage(),
                row.getExpectedAnswerType()
        );
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagRetrievalEvaluationCase item) {
        int topK = item.topK() == null || item.topK() < 1 ? ragProperties.getMilvus().getTopK() : item.topK();
        List<Long> knowledgeBaseIds = item.knowledgeBaseIds() != null && !item.knowledgeBaseIds().isEmpty()
                ? item.knowledgeBaseIds()
                : item.knowledgeBaseId() == null ? List.of() : List.of(item.knowledgeBaseId());
        List<RagSearchItem> retrieved = retrievalService.retrieve(new RagRetrievalCriteria(
                item.query(),
                item.tenantId() == null ? 0L : item.tenantId(),
                knowledgeBaseIds,
                StringUtils.hasText(item.retrievalMode()) ? item.retrievalMode() : "hybrid",
                topK,
                item.minScore(),
                item.contentTypes(),
                item.permissionTags()
        ));
        List<String> retrievedChunkIds = retrieved.stream()
                .map(RagSearchItem::chunkId)
                .filter(StringUtils::hasText)
                .limit(topK)
                .toList();
        Set<String> expected = new LinkedHashSet<>(item.expectedChunkIds() == null ? List.of() : item.expectedChunkIds());
        int firstHitRank = firstHitRank(expected, retrievedChunkIds);
        double recall = expected.isEmpty()
                ? 0.0
                : retrievedChunkIds.stream().filter(expected::contains).distinct().count() / (double) expected.size();
        RetrievalFailureAnalysisPolicy.FailureAnalysis failure = failureAnalysisPolicy.analyze(
                item.retrievalMode(),
                List.copyOf(expected),
                retrievedChunkIds,
                queryClassificationPolicy.category(item.query(), item.queryCategory()),
                firstHitRank,
                recall
        );
        return new RagRetrievalEvaluationCaseResult(
                item.caseId(),
                item.query(),
                topK,
                List.copyOf(expected),
                retrievedChunkIds,
                firstHitRank > 0,
                firstHitRank > 0 ? 1.0 / firstHitRank : 0.0,
                recall,
                failure.failureType(),
                failure.failureReason(),
                retrievalTraceJson(retrieved),
                failure.clusterKey()
        );
    }

    private Metrics metrics(List<RagRetrievalEvaluationCaseResult> results) {
        if (results.isEmpty()) {
            return new Metrics(0.0, 0.0, 0.0);
        }
        double hitRate = results.stream().filter(RagRetrievalEvaluationCaseResult::hit).count() / (double) results.size();
        double mrr = results.stream().mapToDouble(RagRetrievalEvaluationCaseResult::reciprocalRank).average().orElse(0.0);
        double recall = results.stream().mapToDouble(RagRetrievalEvaluationCaseResult::recall).average().orElse(0.0);
        return new Metrics(hitRate, mrr, recall);
    }

    private RagRetrievalEvalRun persistRun(RagRetrievalEvaluationRequest request, CaseSource source, Metrics metrics) {
        RagRetrievalEvalRun run = new RagRetrievalEvalRun();
        run.setRunNo("eval_" + UUID.randomUUID().toString().replace("-", ""));
        run.setTenantId(firstTenantId(request, source.cases()));
        run.setKnowledgeBaseId(firstKnowledgeBaseId(request, source.cases()));
        run.setVersionTag(firstVersionTag(request, source.cases()));
        run.setRetrievalMode(firstRetrievalMode(request, source.cases()));
        run.setTotalCases(source.cases().size());
        run.setHitRate(metrics.hitRate());
        run.setMeanReciprocalRank(metrics.meanReciprocalRank());
        run.setMeanRecall(metrics.meanRecall());
        run.setSource(source.source());
        run.setCreatedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    private void persistResult(Long runId, Long caseDbId, RagRetrievalEvaluationCaseResult result) {
        RagRetrievalEvalCaseResult row = new RagRetrievalEvalCaseResult();
        row.setRunId(runId);
        row.setCaseDbId(caseDbId);
        row.setCaseId(result.caseId());
        row.setQueryText(result.query());
        row.setTopK(result.topK());
        row.setExpectedChunkIdsJson(writeList(result.expectedChunkIds()));
        row.setRetrievedChunkIdsJson(writeList(result.retrievedChunkIds()));
        row.setHit(result.hit());
        row.setReciprocalRank(result.reciprocalRank());
        row.setRecall(result.recall());
        row.setFailureType(result.failureType());
        row.setFailureReason(result.failureReason());
        row.setRetrievalTraceJson(result.retrievalTraceJson());
        row.setClusterKey(result.clusterKey());
        row.setCreatedAt(LocalDateTime.now());
        resultMapper.insert(row);
    }

    private void persistFailureClusters(Long runId, List<RagRetrievalEvaluationCaseResult> results) {
        Map<String, List<RagRetrievalEvaluationCaseResult>> grouped = results.stream()
                .filter(result -> StringUtils.hasText(result.clusterKey()))
                .collect(Collectors.groupingBy(RagRetrievalEvaluationCaseResult::clusterKey, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<RagRetrievalEvaluationCaseResult>> entry : grouped.entrySet()) {
            List<RagRetrievalEvaluationCaseResult> group = entry.getValue();
            String failureType = group.stream()
                    .map(RagRetrievalEvaluationCaseResult::failureType)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse("UNKNOWN");
            RagRetrievalEvalCluster row = new RagRetrievalEvalCluster();
            row.setRunId(runId);
            row.setClusterKey(entry.getKey());
            row.setClusterLabel(clusterLabel(entry.getKey(), failureType));
            row.setFailureType(failureType);
            row.setCaseCount(group.size());
            row.setSampleCaseIdsJson(writeList(group.stream()
                    .map(RagRetrievalEvaluationCaseResult::caseId)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .toList()));
            row.setSuggestion(failureAnalysisPolicy.suggestion(failureType));
            row.setCreatedAt(LocalDateTime.now());
            clusterMapper.insert(row);
        }
    }

    private int firstHitRank(Set<String> expected, List<String> retrievedChunkIds) {
        if (expected.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < retrievedChunkIds.size(); i++) {
            if (expected.contains(retrievedChunkIds.get(i))) {
                return i + 1;
            }
        }
        return -1;
    }

    private List<RagRetrievalEvaluationCase> loadDefaultCases() {
        Path path = Path.of(ragProperties.getRetrieval().getEvaluationSetPath());
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read retrieval evaluation set: " + path, e);
        }
    }

    private LambdaQueryWrapper<RagRetrievalEvalCase> caseQuery(Long tenantId,
                                                              Long knowledgeBaseId,
                                                              String versionTag,
                                                              Boolean enabled) {
        return new LambdaQueryWrapper<RagRetrievalEvalCase>()
                .eq(RagRetrievalEvalCase::getIsDeleted, 0)
                .eq(tenantId != null, RagRetrievalEvalCase::getTenantId, tenantId)
                .eq(knowledgeBaseId != null, RagRetrievalEvalCase::getKnowledgeBaseId, knowledgeBaseId)
                .eq(StringUtils.hasText(versionTag), RagRetrievalEvalCase::getVersionTag, versionTag)
                .eq(enabled != null, RagRetrievalEvalCase::getEnabled, enabled);
    }

    private LambdaQueryWrapper<RagRetrievalEvalRun> runQuery(Long tenantId,
                                                            Long knowledgeBaseId,
                                                            String versionTag,
                                                            String retrievalMode) {
        return new LambdaQueryWrapper<RagRetrievalEvalRun>()
                .eq(tenantId != null, RagRetrievalEvalRun::getTenantId, tenantId)
                .eq(knowledgeBaseId != null, RagRetrievalEvalRun::getKnowledgeBaseId, knowledgeBaseId)
                .eq(StringUtils.hasText(versionTag), RagRetrievalEvalRun::getVersionTag, versionTag)
                .eq(StringUtils.hasText(retrievalMode), RagRetrievalEvalRun::getRetrievalMode, retrievalMode);
    }

    private void applyCaseOrder(LambdaQueryWrapper<RagRetrievalEvalCase> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getId);
            case "caseId" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getCaseId);
            case "knowledgeBaseId" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getKnowledgeBaseId);
            case "versionTag" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getVersionTag);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getUpdatedAt);
            case "createdAt" -> wrapper.orderBy(true, asc, RagRetrievalEvalCase::getCreatedAt);
            default -> wrapper.orderByDesc(RagRetrievalEvalCase::getUpdatedAt);
        }
    }

    private void applyRunOrder(LambdaQueryWrapper<RagRetrievalEvalRun> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagRetrievalEvalRun::getId);
            case "hitRate" -> wrapper.orderBy(true, asc, RagRetrievalEvalRun::getHitRate);
            case "meanReciprocalRank" -> wrapper.orderBy(true, asc, RagRetrievalEvalRun::getMeanReciprocalRank);
            case "meanRecall" -> wrapper.orderBy(true, asc, RagRetrievalEvalRun::getMeanRecall);
            case "createdAt" -> wrapper.orderBy(true, asc, RagRetrievalEvalRun::getCreatedAt);
            default -> wrapper.orderByDesc(RagRetrievalEvalRun::getCreatedAt);
        }
    }

    private Long firstTenantId(RagRetrievalEvaluationRequest request, List<RagRetrievalEvaluationCase> cases) {
        if (request != null && request.tenantId() != null) {
            return request.tenantId();
        }
        return cases.stream().map(RagRetrievalEvaluationCase::tenantId).filter(Objects::nonNull).findFirst().orElse(0L);
    }

    private Long firstKnowledgeBaseId(RagRetrievalEvaluationRequest request, List<RagRetrievalEvaluationCase> cases) {
        if (request != null && request.knowledgeBaseId() != null) {
            return request.knowledgeBaseId();
        }
        return cases.stream()
                .map(item -> item.knowledgeBaseId() != null
                        ? item.knowledgeBaseId()
                        : item.knowledgeBaseIds() == null || item.knowledgeBaseIds().isEmpty() ? null : item.knowledgeBaseIds().get(0))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String firstVersionTag(RagRetrievalEvaluationRequest request, List<RagRetrievalEvaluationCase> cases) {
        if (request != null && StringUtils.hasText(request.versionTag())) {
            return request.versionTag().trim();
        }
        return cases.stream()
                .map(RagRetrievalEvaluationCase::versionTag)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("default");
    }

    private String firstRetrievalMode(RagRetrievalEvaluationRequest request, List<RagRetrievalEvaluationCase> cases) {
        if (request != null && StringUtils.hasText(request.retrievalMode())) {
            return request.retrievalMode().trim();
        }
        return cases.stream()
                .map(RagRetrievalEvaluationCase::retrievalMode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("hybrid");
    }

    private String reportKey(RagRetrievalEvalRun run) {
        return stringValue(run.getKnowledgeBaseId()) + "|" + stringValue(run.getVersionTag()) + "|" + stringValue(run.getRetrievalMode());
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize retrieval evaluation list", e);
        }
    }

    private String retrievalTraceJson(List<RagSearchItem> retrieved) {
        try {
            List<Map<String, Object>> trace = retrieved == null ? List.of() : retrieved.stream()
                    .map(item -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("rank", item.rank());
                        row.put("score", item.score());
                        row.put("chunkId", item.chunkId());
                        row.put("documentId", item.documentId());
                        row.put("knowledgeBaseId", item.knowledgeBaseId());
                        row.put("retrievalMode", item.metadata() == null ? null : item.metadata().get("retrieval_mode"));
                        row.entrySet().removeIf(entry -> entry.getValue() == null);
                        return row;
                    })
                    .toList();
            return objectMapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            return "[]";
        }
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

    private String defaultRetrievalMode(String value) {
        return StringUtils.hasText(value) ? value.trim() : "hybrid";
    }

    private String defaultVersionTag(String value) {
        return StringUtils.hasText(value) ? value.trim() : "default";
    }

    private String defaultExpectedAnswerType(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "fact";
    }

    private String clusterLabel(String clusterKey, String failureType) {
        if (!StringUtils.hasText(clusterKey)) {
            return failureType;
        }
        String[] parts = clusterKey.split(":");
        if (parts.length >= 3) {
            return parts[0] + " / " + parts[1] + " / " + parts[2];
        }
        return failureType;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record CaseSource(List<RagRetrievalEvaluationCase> cases, Map<String, Long> caseDbIdsByCaseId, String source) {
    }

    private record Metrics(double hitRate, double meanReciprocalRank, double meanRecall) {
    }
}

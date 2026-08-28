package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.model.query.*;
import cc.ivera.ragdemo.service.query.*;
import cc.ivera.ragdemo.util.TraceUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG 查询与审计", description = "RAG 查询、搜索、日志、命中明细、反馈和导出接口")
public class RagQueryController {

    private final RagQueryService ragQueryService;
    private final RagQueryLogService queryLogService;
    private final RagRetrievalEvaluationService evaluationService;
    private final RerankObservationService rerankObservationService;
    private final RetrievalEvaluationTrendService evaluationTrendService;
    private final RerankObservationTrendService rerankTrendService;
    private final KeywordIndexTemplateService keywordIndexTemplateService;
    private final KeywordIndexHealthService keywordIndexHealthService;
    private final KeywordReindexService keywordReindexService;
    private final MaterializedMetricAggregationService materializedMetricAggregationService;
    private final QueryCostAnalyticsService queryCostAnalyticsService;
    private final ModelPricingService modelPricingService;
    private final QueryFeedbackWorkflowService feedbackWorkflowService;
    private final QueryFeedbackTrendService feedbackTrendService;
    private final FeedbackRevisionTaskService revisionTaskService;

    @PostMapping("/query")
    @Operation(summary = "执行 RAG 问答", description = "按知识库、检索模式、召回参数执行 RAG 问答，并写入查询日志。")
    public RagApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        String traceId = TraceUtils.currentTraceId();
        return RagApiResponse.ok(traceId, ragQueryService.query(request, traceId));
    }

    @PostMapping("/search")
    @Operation(summary = "执行 RAG 检索", description = "只返回召回结果和命中明细，不生成最终答案。")
    public RagApiResponse<RagSearchResponse> search(@Valid @RequestBody RagSearchRequest request) {
        String traceId = TraceUtils.currentTraceId();
        return RagApiResponse.ok(traceId, ragQueryService.search(request, traceId));
    }

    @PostMapping("/retrieval-evaluations/run")
    @Operation(summary = "运行检索效果评估", description = "使用请求中的评估用例或默认 JSON 评估集运行检索，返回 hit rate、MRR 和 recall 指标。")
    public RagApiResponse<RagRetrievalEvaluationResponse> runRetrievalEvaluation(@RequestBody(required = false) RagRetrievalEvaluationRequest request) {
        return RagApiResponse.ok(evaluationService.evaluate(request));
    }

    @GetMapping("/retrieval-evaluations/cases")
    @Operation(summary = "List retrieval evaluation cases", description = "Page stored retrieval evaluation cases by tenant, knowledge base, version tag and enabled flag.")
    public RagApiResponse<PageResponse<?>> listRetrievalEvaluationCases(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                        @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                        @RequestParam(value = "versionTag", required = false) String versionTag,
                                                                        @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                                        @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                        @RequestParam(value = "limit", required = false) Integer limit,
                                                                        @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                        @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(evaluationService.pageCases(
                tenantId,
                knowledgeBaseId,
                versionTag,
                enabled,
                pageNo,
                pageSize == null ? limit : pageSize,
                sortBy,
                sortDirection
        ));
    }

    @PostMapping("/retrieval-evaluations/cases")
    @Operation(summary = "Save retrieval evaluation case", description = "Create or update one retrieval evaluation case with expected chunk ids.")
    public RagApiResponse<?> saveRetrievalEvaluationCase(@RequestBody RagRetrievalEvaluationCaseUpsertRequest request) {
        return RagApiResponse.ok(evaluationService.saveCase(request));
    }

    @DeleteMapping("/retrieval-evaluations/cases/{caseId}")
    @Operation(summary = "Delete retrieval evaluation case", description = "Soft delete a stored retrieval evaluation case.")
    public RagApiResponse<?> deleteRetrievalEvaluationCase(@PathVariable Long caseId) {
        evaluationService.deleteCase(caseId);
        return RagApiResponse.ok(java.util.Map.of("deleted", true));
    }

    @GetMapping("/retrieval-evaluations/runs")
    @Operation(summary = "List retrieval evaluation runs", description = "Page evaluation history by tenant, knowledge base, version tag and retrieval mode.")
    public RagApiResponse<PageResponse<?>> listRetrievalEvaluationRuns(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                       @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                       @RequestParam(value = "versionTag", required = false) String versionTag,
                                                                       @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                                       @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                       @RequestParam(value = "limit", required = false) Integer limit,
                                                                       @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                       @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(evaluationService.pageRuns(
                tenantId,
                knowledgeBaseId,
                versionTag,
                retrievalMode,
                pageNo,
                pageSize == null ? limit : pageSize,
                sortBy,
                sortDirection
        ));
    }

    @GetMapping("/retrieval-evaluations/runs/{runId}")
    @Operation(summary = "Get retrieval evaluation run detail", description = "Return one evaluation run and its per-case results.")
    public RagApiResponse<RagRetrievalEvaluationRunDetailResponse> getRetrievalEvaluationRun(@PathVariable Long runId) {
        return RagApiResponse.ok(evaluationService.runDetail(runId));
    }

    @GetMapping("/retrieval-evaluations/report")
    @Operation(summary = "Get retrieval evaluation report", description = "Compare evaluation metrics grouped by knowledge base, version tag and retrieval mode.")
    public RagApiResponse<List<RagRetrievalEvaluationReportItem>> retrievalEvaluationReport(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                                           @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                                           @RequestParam(value = "retrievalMode", required = false) String retrievalMode) {
        return RagApiResponse.ok(evaluationService.report(tenantId, knowledgeBaseId, retrievalMode));
    }

    @GetMapping("/keyword-index/templates")
    @Operation(summary = "Preview keyword index template", description = "Return analyzer profiles and the rendered Elasticsearch template for the active configuration.")
    public RagApiResponse<?> keywordIndexTemplates() {
        return RagApiResponse.ok(java.util.Map.of(
                "profiles", keywordIndexTemplateService.analyzerProfiles(),
                "current", keywordIndexTemplateService.currentTemplate()
        ));
    }

    @PostMapping("/keyword-index/templates/apply")
    @Operation(summary = "Render keyword index template apply payload", description = "Return the rendered template payload; production deployments can apply it explicitly through index governance.")
    public RagApiResponse<?> keywordIndexTemplateApplyPayload() {
        return RagApiResponse.ok(keywordIndexTemplateService.currentTemplate());
    }

    @PostMapping("/keyword-index/aliases/switch")
    @Operation(summary = "Plan keyword index alias switch", description = "Build the Elasticsearch _aliases payload for switching the active keyword index alias.")
    public RagApiResponse<?> keywordIndexAliasSwitchPlan(@RequestParam(value = "fromIndex", required = false) String fromIndex,
                                                        @RequestParam("toIndex") String toIndex) {
        return RagApiResponse.ok(keywordIndexTemplateService.aliasSwitchPlan(fromIndex, toIndex));
    }

    @GetMapping("/keyword-index/health")
    @Operation(summary = "Get keyword index configuration health", description = "Return configured provider, engine compatibility, active alias and analyzer profile.")
    public RagApiResponse<?> keywordIndexHealth() {
        return RagApiResponse.ok(keywordIndexHealthService.health());
    }

    @PostMapping("/keyword-index/reindex-jobs")
    @Operation(summary = "Create keyword reindex job", description = "Create a tenant-scoped Elasticsearch reindex job.")
    public RagApiResponse<?> createKeywordReindexJob(@RequestBody(required = false) KeywordReindexJobRequest request) {
        return RagApiResponse.ok(keywordReindexService.create(request));
    }

    @GetMapping("/keyword-index/reindex-jobs")
    @Operation(summary = "List keyword reindex jobs", description = "List tenant-scoped keyword reindex jobs.")
    public RagApiResponse<?> listKeywordReindexJobs(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        return RagApiResponse.ok(keywordReindexService.list(tenantId, limit));
    }

    @GetMapping("/keyword-index/reindex-jobs/{jobId}")
    @Operation(summary = "Get keyword reindex job", description = "Return one keyword reindex job.")
    public RagApiResponse<?> keywordReindexJob(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.detail(jobId));
    }

    @GetMapping("/keyword-index/reindex-jobs/{jobId}/preview")
    @Operation(summary = "Preview keyword reindex job", description = "Return chunk counts and alias switch plan.")
    public RagApiResponse<?> previewKeywordReindexJob(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.preview(jobId));
    }

    @PostMapping("/keyword-index/reindex-jobs/{jobId}/run")
    @Operation(summary = "Run keyword reindex job", description = "Create target index, backfill chunks and run sample validation.")
    public RagApiResponse<?> runKeywordReindexJob(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.run(jobId));
    }

    @PostMapping("/keyword-index/reindex-jobs/{jobId}/switch-alias")
    @Operation(summary = "Switch keyword index alias", description = "Execute atomic alias switch after validation.")
    public RagApiResponse<?> switchKeywordReindexAlias(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.switchAlias(jobId));
    }

    @PostMapping("/keyword-index/reindex-jobs/{jobId}/rollback")
    @Operation(summary = "Rollback keyword index alias", description = "Rollback alias to the recorded previous index.")
    public RagApiResponse<?> rollbackKeywordReindexJob(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.rollback(jobId));
    }

    @PostMapping("/keyword-index/reindex-jobs/{jobId}/cancel")
    @Operation(summary = "Cancel keyword reindex job", description = "Mark a planned or running keyword reindex job as cancelled/failed.")
    public RagApiResponse<?> cancelKeywordReindexJob(@PathVariable Long jobId) {
        return RagApiResponse.ok(keywordReindexService.cancel(jobId));
    }

    @GetMapping("/retrieval-evaluations/trends")
    @Operation(summary = "Get retrieval evaluation trends", description = "Aggregate hit rate, MRR and recall by time window and query dimensions.")
    public RagApiResponse<?> retrievalEvaluationTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                       @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                       @RequestParam(value = "versionTag", required = false) String versionTag,
                                                       @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                       @RequestParam(value = "queryCategory", required = false) String queryCategory,
                                                       @RequestParam(value = "language", required = false) String language,
                                                       @RequestParam(value = "difficultyLevel", required = false) String difficultyLevel,
                                                       @RequestParam(value = "window", required = false) String window,
                                                       @RequestParam(value = "from", required = false) String from,
                                                       @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(evaluationTrendService.trends(
                tenantId, knowledgeBaseId, versionTag, retrievalMode, queryCategory, language, difficultyLevel,
                window, parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/retrieval-evaluations/slices")
    @Operation(summary = "Get retrieval evaluation slices", description = "Aggregate retrieval evaluation metrics by queryCategory, language, difficultyLevel, retrievalMode, versionTag or topK.")
    public RagApiResponse<?> retrievalEvaluationSlices(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                       @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                       @RequestParam(value = "versionTag", required = false) String versionTag,
                                                       @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                       @RequestParam(value = "dimension", required = false) String dimension,
                                                       @RequestParam(value = "from", required = false) String from,
                                                       @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(evaluationTrendService.slices(
                tenantId, knowledgeBaseId, versionTag, retrievalMode, dimension, parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/retrieval-evaluations/failure-clusters")
    @Operation(summary = "Get retrieval failure clusters", description = "Group failed evaluation cases by deterministic failure cluster key.")
    public RagApiResponse<?> retrievalFailureClusters(@RequestParam(value = "runId", required = false) Long runId,
                                                      @RequestParam(value = "tenantId", required = false) Long tenantId,
                                                      @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                      @RequestParam(value = "retrievalMode", required = false) String retrievalMode) {
        return RagApiResponse.ok(evaluationTrendService.failureClusters(runId, tenantId, knowledgeBaseId, retrievalMode));
    }

    @GetMapping("/retrieval-evaluations/failure-clusters/{clusterKey}/cases")
    @Operation(summary = "List failed cases in one cluster", description = "Page evaluation result rows for a deterministic failure cluster.")
    public RagApiResponse<PageResponse<?>> retrievalFailureClusterCases(@PathVariable String clusterKey,
                                                                        @RequestParam(value = "runId", required = false) Long runId,
                                                                        @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                        @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                        @RequestParam(value = "limit", required = false) Integer limit,
                                                                        @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                        @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(evaluationTrendService.failureClusterCases(
                clusterKey, runId, pageNo, pageSize == null ? limit : pageSize, sortBy, sortDirection
        ));
    }

    @GetMapping("/retrieval-evaluations/compare-runs")
    @Operation(summary = "Compare retrieval evaluation runs", description = "Return metric deltas between two stored evaluation runs.")
    public RagApiResponse<?> compareRetrievalEvaluationRuns(@RequestParam("leftRunId") Long leftRunId,
                                                            @RequestParam("rightRunId") Long rightRunId) {
        return RagApiResponse.ok(evaluationTrendService.compareRuns(leftRunId, rightRunId));
    }

    @GetMapping("/rerank-observability/summary")
    @Operation(summary = "Get rerank observability summary", description = "Aggregate DashScope rerank latency, failures, fallback degradation and estimated cost.")
    public RagApiResponse<RagRerankObservationSummary> rerankObservationSummary(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                               @RequestParam(value = "provider", required = false) String provider,
                                                                               @RequestParam(value = "model", required = false) String model) {
        return RagApiResponse.ok(rerankObservationService.summary(tenantId, provider, model));
    }

    @GetMapping("/rerank-observability/trends")
    @Operation(summary = "Get rerank observability trends", description = "Aggregate rerank requests by time window with latency, failure, fallback, token and cost metrics.")
    public RagApiResponse<?> rerankObservationTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                     @RequestParam(value = "provider", required = false) String provider,
                                                     @RequestParam(value = "model", required = false) String model,
                                                     @RequestParam(value = "apiKeyHash", required = false) String apiKeyHash,
                                                     @RequestParam(value = "errorCode", required = false) String errorCode,
                                                     @RequestParam(value = "degradedReason", required = false) String degradedReason,
                                                     @RequestParam(value = "window", required = false) String window,
                                                     @RequestParam(value = "from", required = false) String from,
                                                     @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.trends(
                tenantId, provider, model, apiKeyHash, errorCode, degradedReason, window, parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/rerank-observability/by-api-key")
    @Operation(summary = "Aggregate rerank calls by API key hash", description = "Return request, failure, fallback, latency, token and cost metrics grouped by API key hash.")
    public RagApiResponse<?> rerankByApiKey(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                            @RequestParam(value = "provider", required = false) String provider,
                                            @RequestParam(value = "model", required = false) String model,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.byDimension("apiKeyHash", tenantId, provider, model, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/by-tenant")
    @Operation(summary = "Aggregate rerank calls by tenant", description = "Return rerank observability metrics grouped by tenant.")
    public RagApiResponse<?> rerankByTenant(@RequestParam(value = "provider", required = false) String provider,
                                            @RequestParam(value = "model", required = false) String model,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.byDimension("tenant", null, provider, model, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/by-error-code")
    @Operation(summary = "Aggregate rerank calls by normalized error code", description = "Return rerank observability metrics grouped by normalized error code.")
    public RagApiResponse<?> rerankByErrorCode(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                               @RequestParam(value = "provider", required = false) String provider,
                                               @RequestParam(value = "model", required = false) String model,
                                               @RequestParam(value = "from", required = false) String from,
                                               @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.byDimension("errorCode", tenantId, provider, model, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/latency-percentiles")
    @Operation(summary = "Get rerank latency percentiles", description = "Return p50/p90/p99 latency for matching rerank calls.")
    public RagApiResponse<?> rerankLatencyPercentiles(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                      @RequestParam(value = "provider", required = false) String provider,
                                                      @RequestParam(value = "model", required = false) String model,
                                                      @RequestParam(value = "from", required = false) String from,
                                                      @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.latencyPercentiles(tenantId, provider, model, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/cost-trends")
    @Operation(summary = "Get rerank cost trends", description = "Return time-window rerank trend rows including estimatedCost.")
    public RagApiResponse<?> rerankCostTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                              @RequestParam(value = "provider", required = false) String provider,
                                              @RequestParam(value = "model", required = false) String model,
                                              @RequestParam(value = "window", required = false) String window,
                                              @RequestParam(value = "from", required = false) String from,
                                              @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.trends(tenantId, provider, model, null, null, null, window, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/degradation-trends")
    @Operation(summary = "Get rerank degradation trends", description = "Return time-window rerank trend rows filtered by degradation reason when provided.")
    public RagApiResponse<?> rerankDegradationTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                     @RequestParam(value = "provider", required = false) String provider,
                                                     @RequestParam(value = "model", required = false) String model,
                                                     @RequestParam(value = "degradedReason", required = false) String degradedReason,
                                                     @RequestParam(value = "window", required = false) String window,
                                                     @RequestParam(value = "from", required = false) String from,
                                                     @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(rerankTrendService.trends(tenantId, provider, model, null, null, degradedReason, window, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/rerank-observability/logs")
    @Operation(summary = "List rerank call logs", description = "Page individual rerank call observations with latency, token, cost, failure and fallback fields.")
    public RagApiResponse<PageResponse<?>> listRerankObservationLogs(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                     @RequestParam(value = "provider", required = false) String provider,
                                                                     @RequestParam(value = "model", required = false) String model,
                                                                     @RequestParam(value = "success", required = false) Boolean success,
                                                                     @RequestParam(value = "fallback", required = false) Boolean fallback,
                                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                     @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                     @RequestParam(value = "limit", required = false) Integer limit,
                                                                     @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                     @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(rerankObservationService.pageLogs(
                tenantId,
                provider,
                model,
                success,
                fallback,
                pageNo,
                pageSize == null ? limit : pageSize,
                sortBy,
                sortDirection
        ));
    }

    @PostMapping("/metrics/materialized/backfill")
    @Operation(summary = "Backfill materialized metrics", description = "Aggregate query cost, feedback quality and rerank observation metrics into materialized tables.")
    public RagApiResponse<?> backfillMaterializedMetrics(@RequestParam(value = "from", required = false) String from,
                                                         @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(materializedMetricAggregationService.aggregate(parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/metrics/materialized/watermarks")
    @Operation(summary = "List materialized metric watermarks", description = "Return aggregation watermarks for materialized metric jobs.")
    public RagApiResponse<?> materializedMetricWatermarks() {
        return RagApiResponse.ok(materializedMetricAggregationService.watermarks());
    }

    @GetMapping("/query-logs")
    @Operation(summary = "分页查询 RAG 查询日志", description = "支持租户、类型、状态、会话、trace、查询文本过滤，以及 pageNo/pageSize/limit/sortBy/sortDirection。")
    public RagApiResponse<PageResponse<?>> listQueryLogs(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                         @RequestParam(value = "queryType", required = false) String queryType,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "conversationId", required = false) String conversationId,
                                                         @RequestParam(value = "traceId", required = false) String traceId,
                                                         @RequestParam(value = "queryText", required = false) String queryText,
                                                         @RequestParam(value = "visibility", required = false) String visibility,
                                                         @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                         @RequestParam(value = "limit", required = false) Integer limit,
                                                         @RequestParam(value = "sortBy", required = false) String sortBy,
                                                         @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<?> page = queryLogService.pageLogs(
                tenantId,
                queryType,
                status,
                conversationId,
                traceId,
                queryText,
                pageNo,
                pageSize == null ? limit : pageSize,
                sortBy,
                sortDirection,
                visibility
        );
        return RagApiResponse.ok(page);
    }

    @GetMapping("/query-logs/export")
    @Operation(summary = "导出 RAG 查询日志 CSV", description = "按筛选条件导出 CSV 文件，响应保持文件下载格式。")
    public ResponseEntity<String> exportQueryLogs(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                  @RequestParam(value = "queryType", required = false) String queryType,
                                                  @RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "conversationId", required = false) String conversationId,
                                                  @RequestParam(value = "traceId", required = false) String traceId,
                                                  @RequestParam(value = "queryText", required = false) String queryText,
                                                  @RequestParam(value = "limit", required = false) Integer limit) {
        String csv = queryLogService.exportLogsCsv(tenantId, queryType, status, conversationId, traceId, queryText, limit);
        String fileName = "rag-query-logs-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(csv);
    }

    @GetMapping("/query-logs/feedback-summary")
    @Operation(summary = "查询反馈汇总", description = "按查询日志筛选条件聚合反馈数量，并返回最近反馈明细。")
    public RagApiResponse<RagFeedbackSummaryResponse> feedbackSummary(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                      @RequestParam(value = "queryType", required = false) String queryType,
                                                                      @RequestParam(value = "status", required = false) String status,
                                                                      @RequestParam(value = "conversationId", required = false) String conversationId,
                                                                      @RequestParam(value = "traceId", required = false) String traceId,
                                                                      @RequestParam(value = "rating", required = false) String rating,
                                                                      @RequestParam(value = "createdBy", required = false) String createdBy,
                                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        RagFeedbackSummaryResponse summary = queryLogService.feedbackSummary(
                tenantId,
                queryType,
                status,
                conversationId,
                traceId,
                rating,
                createdBy,
                limit
        );
        return RagApiResponse.ok(summary);
    }

    @PostMapping("/query-logs/delete")
    @Operation(summary = "Soft delete query logs", description = "Mark matching query logs as deleted and create a delete audit record.")
    public RagApiResponse<RagQueryLogOperationResponse> softDeleteQueryLogs(@RequestBody(required = false) RagQueryLogOperationRequest request) {
        return RagApiResponse.ok(queryLogService.softDelete(request));
    }

    @PostMapping("/query-logs/archive")
    @Operation(summary = "Archive query logs", description = "Copy matching query logs into archive storage and mark them archived.")
    public RagApiResponse<RagQueryLogOperationResponse> archiveQueryLogs(@RequestBody(required = false) RagQueryLogOperationRequest request) {
        return RagApiResponse.ok(queryLogService.archive(request));
    }

    @PostMapping("/query-logs/restore")
    @Operation(summary = "Restore query logs", description = "Restore soft-deleted or archived query logs to active status.")
    public RagApiResponse<RagQueryLogOperationResponse> restoreQueryLogs(@RequestBody(required = false) RagQueryLogOperationRequest request) {
        return RagApiResponse.ok(queryLogService.restore(request));
    }

    @PostMapping("/query-logs/purge")
    @Operation(summary = "Physically purge query logs", description = "Physically delete query logs only when retention rules allow it.")
    public RagApiResponse<RagQueryLogOperationResponse> purgeQueryLogs(@RequestBody(required = false) RagQueryLogOperationRequest request) {
        return RagApiResponse.ok(queryLogService.purge(request));
    }

    @GetMapping("/query-logs/delete-audits")
    @Operation(summary = "List query log delete audits", description = "Page delete, archive, restore and purge audit records.")
    public RagApiResponse<PageResponse<?>> listQueryLogDeleteAudits(@RequestParam(value = "deleteNo", required = false) String deleteNo,
                                                                    @RequestParam(value = "mode", required = false) String mode,
                                                                    @RequestParam(value = "operator", required = false) String operator,
                                                                    @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return RagApiResponse.ok(queryLogService.pageDeleteAudits(deleteNo, mode, operator, pageNo, pageSize));
    }

    @GetMapping("/query-logs/delete-audits/{deleteNo}")
    @Operation(summary = "Get query log delete audit", description = "Return one query log operation audit by deleteNo.")
    public RagApiResponse<?> getQueryLogDeleteAudit(@PathVariable String deleteNo) {
        return RagApiResponse.ok(queryLogService.getDeleteAudit(deleteNo));
    }

    @GetMapping("/query-retention-policies")
    @Operation(summary = "List query retention policies", description = "Page retention policies used by query log governance.")
    public RagApiResponse<PageResponse<?>> listQueryRetentionPolicies(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                     @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                     @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return RagApiResponse.ok(queryLogService.pageRetentionPolicies(tenantId, enabled, pageNo, pageSize));
    }

    @PostMapping("/query-retention-policies")
    @Operation(summary = "Create query retention policy", description = "Create a retention policy by tenant, query type and status.")
    public RagApiResponse<?> createQueryRetentionPolicy(@RequestBody RagQueryRetentionPolicyRequest request) {
        return RagApiResponse.ok(queryLogService.createRetentionPolicy(request));
    }

    @org.springframework.web.bind.annotation.PutMapping("/query-retention-policies/{id}")
    @Operation(summary = "Update query retention policy", description = "Update an existing query retention policy.")
    public RagApiResponse<?> updateQueryRetentionPolicy(@PathVariable Long id,
                                                       @RequestBody RagQueryRetentionPolicyRequest request) {
        return RagApiResponse.ok(queryLogService.updateRetentionPolicy(id, request));
    }

    @GetMapping("/query-costs/trends")
    @Operation(summary = "Get query cost trends", description = "Aggregate query count, token and estimated cost by time window.")
    public RagApiResponse<?> queryCostTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                            @RequestParam(value = "queryType", required = false) String queryType,
                                            @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                            @RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "llmModel", required = false) String llmModel,
                                            @RequestParam(value = "embeddingModel", required = false) String embeddingModel,
                                            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                            @RequestParam(value = "window", required = false) String window,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(queryCostAnalyticsService.trends(
                tenantId, queryType, retrievalMode, status, llmModel, embeddingModel, knowledgeBaseId,
                window, parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/query-costs/by-model")
    @Operation(summary = "Aggregate query costs by model", description = "Return token and estimated cost grouped by LLM model.")
    public RagApiResponse<?> queryCostsByModel(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                               @RequestParam(value = "queryType", required = false) String queryType,
                                               @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "from", required = false) String from,
                                               @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(queryCostAnalyticsService.byDimension("llmModel", tenantId, queryType, retrievalMode, status, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/query-costs/by-tenant")
    @Operation(summary = "Aggregate query costs by tenant", description = "Return token and estimated cost grouped by tenant.")
    public RagApiResponse<?> queryCostsByTenant(@RequestParam(value = "queryType", required = false) String queryType,
                                                @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                @RequestParam(value = "status", required = false) String status,
                                                @RequestParam(value = "from", required = false) String from,
                                                @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(queryCostAnalyticsService.byDimension("tenant", null, queryType, retrievalMode, status, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/query-costs/anomalies")
    @Operation(summary = "Get query cost anomalies", description = "Return persisted cost anomalies plus live token-spike detection.")
    public RagApiResponse<?> queryCostAnomalies(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                @RequestParam(value = "from", required = false) String from,
                                                @RequestParam(value = "to", required = false) String to,
                                                @RequestParam(value = "tokenThreshold", required = false) Integer tokenThreshold) {
        return RagApiResponse.ok(queryCostAnalyticsService.anomalies(tenantId, parseDateTime(from), parseDateTime(to), tokenThreshold, null));
    }

    @GetMapping("/query-costs/export")
    @Operation(summary = "Export query cost trends", description = "Export query cost trend rows as CSV.")
    public ResponseEntity<String> exportQueryCosts(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                   @RequestParam(value = "queryType", required = false) String queryType,
                                                   @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   @RequestParam(value = "window", required = false) String window,
                                                   @RequestParam(value = "from", required = false) String from,
                                                   @RequestParam(value = "to", required = false) String to) {
        String csv = queryCostAnalyticsService.exportCsv(tenantId, queryType, retrievalMode, status, window, parseDateTime(from), parseDateTime(to));
        String fileName = "rag-query-costs-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".csv";
        return csvResponse(fileName, csv);
    }

    @GetMapping("/model-pricing")
    @Operation(summary = "List model pricing", description = "Page model pricing records used by estimated query cost calculation.")
    public RagApiResponse<PageResponse<?>> listModelPricing(@RequestParam(value = "provider", required = false) String provider,
                                                           @RequestParam(value = "model", required = false) String model,
                                                           @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                           @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return RagApiResponse.ok(modelPricingService.page(provider, model, enabled, pageNo, pageSize));
    }

    @PostMapping("/model-pricing")
    @Operation(summary = "Create model pricing", description = "Create a provider/model token price record.")
    public RagApiResponse<?> createModelPricing(@RequestBody RagModelPricingRequest request) {
        return RagApiResponse.ok(modelPricingService.create(request));
    }

    @org.springframework.web.bind.annotation.PutMapping("/model-pricing/{id}")
    @Operation(summary = "Update model pricing", description = "Update a provider/model token price record.")
    public RagApiResponse<?> updateModelPricing(@PathVariable Long id,
                                                @RequestBody RagModelPricingRequest request) {
        return RagApiResponse.ok(modelPricingService.update(id, request));
    }

    @GetMapping("/query-feedback")
    @Operation(summary = "List query feedback work queue", description = "Page feedback items by status, assignee, priority and query context.")
    public RagApiResponse<PageResponse<?>> listQueryFeedback(@RequestParam(value = "queryLogId", required = false) Long queryLogId,
                                                            @RequestParam(value = "tenantId", required = false) Long tenantId,
                                                            @RequestParam(value = "rating", required = false) String rating,
                                                            @RequestParam(value = "feedbackStatus", required = false) String feedbackStatus,
                                                            @RequestParam(value = "priority", required = false) String priority,
                                                            @RequestParam(value = "assignee", required = false) String assignee,
                                                            @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                            @RequestParam(value = "sortBy", required = false) String sortBy,
                                                            @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(feedbackWorkflowService.page(queryLogId, tenantId, rating, feedbackStatus, priority, assignee, pageNo, pageSize, sortBy, sortDirection));
    }

    @GetMapping("/query-feedback/trends")
    @Operation(summary = "Get feedback quality trends", description = "Aggregate feedback quality and closed-loop repair metrics by time window.")
    public RagApiResponse<?> feedbackTrends(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                            @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                            @RequestParam(value = "queryType", required = false) String queryType,
                                            @RequestParam(value = "feedbackRating", required = false) String feedbackRating,
                                            @RequestParam(value = "feedbackStatus", required = false) String feedbackStatus,
                                            @RequestParam(value = "assignee", required = false) String assignee,
                                            @RequestParam(value = "window", required = false) String window,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(feedbackTrendService.trends(
                tenantId, knowledgeBaseId, retrievalMode, queryType, feedbackRating, feedbackStatus, assignee,
                window, parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/query-feedback/quality-summary")
    @Operation(summary = "Get feedback quality summary", description = "Return aggregate feedback quality metrics for filters.")
    public RagApiResponse<?> feedbackQualitySummary(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                   @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                   @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                   @RequestParam(value = "queryType", required = false) String queryType,
                                                   @RequestParam(value = "feedbackRating", required = false) String feedbackRating,
                                                   @RequestParam(value = "feedbackStatus", required = false) String feedbackStatus,
                                                   @RequestParam(value = "assignee", required = false) String assignee,
                                                   @RequestParam(value = "from", required = false) String from,
                                                   @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(feedbackTrendService.qualitySummary(
                tenantId, knowledgeBaseId, retrievalMode, queryType, feedbackRating, feedbackStatus, assignee,
                parseDateTime(from), parseDateTime(to)
        ));
    }

    @GetMapping("/query-feedback/by-knowledge-base")
    @Operation(summary = "Aggregate feedback by knowledge base", description = "Return feedback quality grouped by knowledge base ID list.")
    public RagApiResponse<?> feedbackByKnowledgeBase(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                     @RequestParam(value = "from", required = false) String from,
                                                     @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(feedbackTrendService.byDimension("knowledgeBase", tenantId, null, null, null, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/query-feedback/by-assignee")
    @Operation(summary = "Aggregate feedback by assignee", description = "Return feedback quality grouped by assignee.")
    public RagApiResponse<?> feedbackByAssignee(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                @RequestParam(value = "from", required = false) String from,
                                                @RequestParam(value = "to", required = false) String to) {
        return RagApiResponse.ok(feedbackTrendService.byDimension("assignee", tenantId, null, null, null, parseDateTime(from), parseDateTime(to)));
    }

    @GetMapping("/query-feedback/export")
    @Operation(summary = "Export feedback quality trends", description = "Export feedback quality trend rows as CSV.")
    public ResponseEntity<String> exportFeedbackQuality(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                        @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                        @RequestParam(value = "retrievalMode", required = false) String retrievalMode,
                                                        @RequestParam(value = "queryType", required = false) String queryType,
                                                        @RequestParam(value = "window", required = false) String window,
                                                        @RequestParam(value = "from", required = false) String from,
                                                        @RequestParam(value = "to", required = false) String to) {
        String csv = feedbackTrendService.exportCsv(tenantId, knowledgeBaseId, retrievalMode, queryType, window, parseDateTime(from), parseDateTime(to));
        String fileName = "rag-query-feedback-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".csv";
        return csvResponse(fileName, csv);
    }

    @GetMapping("/query-feedback/{id}")
    @Operation(summary = "Get query feedback detail", description = "Return one feedback work item.")
    public RagApiResponse<?> getQueryFeedback(@PathVariable Long id) {
        return RagApiResponse.ok(feedbackWorkflowService.detail(id));
    }

    @PostMapping("/query-feedback/{id}/assign")
    @Operation(summary = "Assign query feedback", description = "Assign a feedback work item to an owner and record an event.")
    public RagApiResponse<?> assignQueryFeedback(@PathVariable Long id,
                                                 @RequestBody RagFeedbackAssignRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.assign(id, request));
    }

    @PostMapping("/query-feedback/{id}/status")
    @Operation(summary = "Change query feedback status", description = "Apply a validated feedback workflow status transition.")
    public RagApiResponse<?> changeQueryFeedbackStatus(@PathVariable Long id,
                                                       @RequestBody RagFeedbackStatusRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.changeStatus(id, request));
    }

    @PostMapping("/query-feedback/{id}/review")
    @Operation(summary = "Review query feedback", description = "Write review result and comment for a feedback item.")
    public RagApiResponse<?> reviewQueryFeedback(@PathVariable Long id,
                                                 @RequestBody RagFeedbackReviewRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.review(id, request));
    }

    @PostMapping("/query-feedback/{id}/comment")
    @Operation(summary = "Comment query feedback", description = "Append a feedback event comment.")
    public RagApiResponse<?> commentQueryFeedback(@PathVariable Long id,
                                                  @RequestBody RagFeedbackCommentRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.comment(id, request));
    }

    @PostMapping("/query-feedback/{id}/close")
    @Operation(summary = "Close query feedback", description = "Close resolved or rejected feedback.")
    public RagApiResponse<?> closeQueryFeedback(@PathVariable Long id,
                                                @RequestBody(required = false) RagFeedbackCommentRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.close(id, request));
    }

    @PostMapping("/query-feedback/{id}/reopen")
    @Operation(summary = "Reopen query feedback", description = "Reopen closed feedback and increment reopened count.")
    public RagApiResponse<?> reopenQueryFeedback(@PathVariable Long id,
                                                 @RequestBody(required = false) RagFeedbackCommentRequest request) {
        return RagApiResponse.ok(feedbackWorkflowService.reopen(id, request));
    }

    @GetMapping("/query-feedback/{id}/events")
    @Operation(summary = "List query feedback events", description = "Return event timeline for a feedback item.")
    public RagApiResponse<?> queryFeedbackEvents(@PathVariable Long id) {
        return RagApiResponse.ok(feedbackWorkflowService.events(id));
    }

    @PostMapping("/query-feedback/{id}/revision-tasks")
    @Operation(summary = "Create revision task from feedback", description = "Create a knowledge-base revision task linked to feedback.")
    public RagApiResponse<?> createRevisionTaskFromFeedback(@PathVariable Long id,
                                                            @RequestBody(required = false) RagFeedbackRevisionTaskRequest request) {
        return RagApiResponse.ok(revisionTaskService.createFromFeedback(id, request));
    }

    @GetMapping("/feedback-revision-tasks")
    @Operation(summary = "List feedback revision tasks", description = "Page revision tasks created from feedback.")
    public RagApiResponse<PageResponse<?>> listRevisionTasks(@RequestParam(value = "feedbackId", required = false) Long feedbackId,
                                                            @RequestParam(value = "tenantId", required = false) Long tenantId,
                                                            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                            @RequestParam(value = "revisionStatus", required = false) String revisionStatus,
                                                            @RequestParam(value = "assignee", required = false) String assignee,
                                                            @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return RagApiResponse.ok(revisionTaskService.page(feedbackId, tenantId, knowledgeBaseId, revisionStatus, assignee, pageNo, pageSize));
    }

    @GetMapping("/feedback-revision-tasks/{id}")
    @Operation(summary = "Get feedback revision task", description = "Return one feedback revision task.")
    public RagApiResponse<?> getRevisionTask(@PathVariable Long id) {
        return RagApiResponse.ok(revisionTaskService.detail(id));
    }

    @PostMapping("/feedback-revision-tasks/{id}/apply")
    @Operation(summary = "Apply feedback revision task", description = "Mark a revision task as applied and store optional after snapshot.")
    public RagApiResponse<?> applyRevisionTask(@PathVariable Long id,
                                               @RequestBody(required = false) RagFeedbackRevisionTaskActionRequest request) {
        return RagApiResponse.ok(revisionTaskService.apply(id, request));
    }

    @PostMapping("/feedback-revision-tasks/{id}/verify")
    @Operation(summary = "Verify feedback revision task", description = "Store verification result and update feedback status.")
    public RagApiResponse<?> verifyRevisionTask(@PathVariable Long id,
                                                @RequestBody(required = false) RagFeedbackRevisionTaskActionRequest request) {
        return RagApiResponse.ok(revisionTaskService.verify(id, request));
    }

    @PostMapping("/feedback-revision-tasks/{id}/reject")
    @Operation(summary = "Reject feedback revision task", description = "Reject a planned or in-progress revision task.")
    public RagApiResponse<?> rejectRevisionTask(@PathVariable Long id,
                                                @RequestBody(required = false) RagFeedbackRevisionTaskActionRequest request) {
        return RagApiResponse.ok(revisionTaskService.reject(id, request));
    }

    @PostMapping("/feedback-revision-tasks/{id}/cancel")
    @Operation(summary = "Cancel feedback revision task", description = "Cancel a planned or in-progress revision task.")
    public RagApiResponse<?> cancelRevisionTask(@PathVariable Long id,
                                                @RequestBody(required = false) RagFeedbackRevisionTaskActionRequest request) {
        return RagApiResponse.ok(revisionTaskService.cancel(id, request));
    }

    @GetMapping("/query-logs/{queryLogId}")
    @Operation(summary = "查询日志详情", description = "返回查询日志、召回命中明细和用户反馈。")
    public RagApiResponse<RagQueryLogDetailResponse> getQueryLog(@PathVariable Long queryLogId) {
        RagQueryLogDetailResponse detail = queryLogService.getDetail(queryLogId);
        return RagApiResponse.ok(detail);
    }

    @PostMapping("/query-logs/{queryLogId}/feedback")
    @Operation(summary = "提交查询反馈", description = "为指定查询日志提交 HELPFUL、NOT_HELPFUL 或 CORRECTION 反馈。")
    public RagApiResponse<?> submitQueryFeedback(@PathVariable Long queryLogId,
                                                 @Valid @RequestBody RagQueryFeedbackRequest request) {
        return RagApiResponse.ok(queryLogService.submitFeedback(queryLogId, request));
    }

    @DeleteMapping("/query-logs/{queryLogId}")
    @Operation(summary = "删除单条查询日志", description = "物理删除指定查询日志及其命中、反馈记录。")
    public RagApiResponse<RagQueryLogDeleteResponse> deleteQueryLog(@PathVariable Long queryLogId) {
        return RagApiResponse.ok(queryLogService.deleteLog(queryLogId));
    }

    @DeleteMapping("/query-logs")
    @Operation(summary = "批量删除查询日志", description = "按 ids 参数批量删除查询日志及其命中、反馈记录。")
    public RagApiResponse<RagQueryLogDeleteResponse> deleteQueryLogs(@RequestParam("ids") List<Long> ids) {
        RagQueryLogDeleteResponse deleted = queryLogService.deleteLogs(ids);
        return RagApiResponse.ok(deleted);
    }

    private ResponseEntity<String> csvResponse(String fileName, String csv) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(csv);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed);
    }
}

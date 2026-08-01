package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.domain.rag.RagKeywordReindexJob;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.mapper.RagKeywordReindexJobMapper;
import cc.ivera.ragdemo.model.query.KeywordIndexAliasSwitchPlan;
import cc.ivera.ragdemo.model.query.KeywordReindexJobRequest;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.util.LogMasker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KeywordReindexService {

    private final RagProperties properties;
    private final RagKeywordReindexJobMapper jobMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final KeywordIndexTemplateService templateService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ReindexStateMachine stateMachine = new ReindexStateMachine();

    @Transactional
    public RagKeywordReindexJob create(KeywordReindexJobRequest request) {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        Long tenantId = request == null || request.tenantId() == null
                ? TenantContextHolder.currentTenantId().orElse(0L)
                : request.tenantId();
        RagKeywordReindexJob job = new RagKeywordReindexJob();
        job.setTenantId(tenantId);
        job.setJobNo("kidx-" + UUID.randomUUID().toString().replace("-", ""));
        job.setSourceIndex(valueOrDefault(request == null ? null : request.sourceIndex(), activeIndex()));
        job.setTargetIndex(valueOrDefault(request == null ? null : request.targetIndex(), targetIndexName(tenantId)));
        job.setAliasName(valueOrDefault(request == null ? null : request.aliasName(), activeAlias()));
        job.setTemplateVersion(valueOrDefault(request == null ? null : request.templateVersion(), config.getIndexVersion()));
        job.setRollbackTarget(job.getSourceIndex());
        job.setJobStatus("PLANNED");
        job.setProgress(0);
        job.setTotalCount(0L);
        job.setSuccessCount(0L);
        job.setFailedCount(0L);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.insert(job);
        return job;
    }

    public Object preview(Long jobId) {
        RagKeywordReindexJob job = required(jobId);
        long count = countChunks(job.getTenantId());
        KeywordIndexAliasSwitchPlan plan = templateService.aliasSwitchPlan(job.getSourceIndex(), job.getTargetIndex());
        return Map.of(
                "job", job,
                "chunkCount", count,
                "aliasSwitchPlan", plan,
                "targetIndex", job.getTargetIndex()
        );
    }

    @Transactional
    public Object run(Long jobId) {
        RagKeywordReindexJob job = required(jobId);
        try {
            mark(job, "CREATING_INDEX", 5, null);
            createTargetIndex(job);
            mark(job, "BACKFILLING", 10, null);
            backfill(job);
            mark(job, "VALIDATING", 90, null);
            validate(job);
            mark(job, "READY_TO_SWITCH", 95, null);
            return detail(job.getId());
        } catch (Exception ex) {
            mark(job, "FAILED", job.getProgress(), ex.getMessage());
            throw ex instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex);
        }
    }

    @Transactional
    public Object switchAlias(Long jobId) {
        RagKeywordReindexJob job = required(jobId);
        mark(job, "SWITCHING_ALIAS", 98, null);
        KeywordIndexAliasSwitchPlan plan = templateService.aliasSwitchPlan(job.getSourceIndex(), job.getTargetIndex());
        request("POST", URI.create(trimTrailingSlash(properties.getKeywordIndex().getBaseUrl()) + plan.requestPath()), plan.requestBody(), false);
        mark(job, "SUCCEEDED", 100, null);
        job = required(jobId);
        job.setFinishedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        return detail(jobId);
    }

    @Transactional
    public Object rollback(Long jobId) {
        RagKeywordReindexJob job = required(jobId);
        mark(job, "ROLLING_BACK", job.getProgress(), null);
        KeywordIndexAliasSwitchPlan plan = templateService.aliasSwitchPlan(job.getTargetIndex(), job.getRollbackTarget());
        request("POST", URI.create(trimTrailingSlash(properties.getKeywordIndex().getBaseUrl()) + plan.requestPath()), plan.requestBody(), false);
        mark(job, "ROLLED_BACK", job.getProgress(), null);
        return detail(jobId);
    }

    @Transactional
    public Object cancel(Long jobId) {
        RagKeywordReindexJob job = required(jobId);
        mark(job, "FAILED", job.getProgress(), "Cancelled by operator");
        return detail(jobId);
    }

    public Object detail(Long jobId) {
        return required(jobId);
    }

    public List<RagKeywordReindexJob> list(Long tenantId, Integer limit) {
        Long effectiveTenantId = tenantId == null ? TenantContextHolder.currentTenantId().orElse(null) : tenantId;
        return jobMapper.selectList(new LambdaQueryWrapper<RagKeywordReindexJob>()
                .eq(effectiveTenantId != null, RagKeywordReindexJob::getTenantId, effectiveTenantId)
                .orderByDesc(RagKeywordReindexJob::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit == null ? 50 : limit, 500))));
    }

    private void createTargetIndex(RagKeywordReindexJob job) {
        String rendered = templateService.currentTemplate().renderedJson()
                .replace(properties.getKeywordIndex().getIndexName(), job.getTargetIndex())
                .replace(activeAlias(), job.getAliasName());
        try {
            request("PUT", indexUri(job.getTargetIndex(), null), indexCreationBody(rendered), false);
        } catch (IllegalStateException ex) {
            // Idempotent: a previous run may have created the target index before the transaction
            // rolled back (e.g. validation failure). ES operations are not transactional, so the
            // index persists. Treat "resource_already_exists_exception" as success so backfill can
            // proceed (PUT _doc/{id} upserts, so re-indexing is also idempotent).
            if (ex.getMessage() != null && ex.getMessage().contains("resource_already_exists_exception")) {
                return;
            }
            throw ex;
        }
    }

    /**
     * The template resource is a composable index template ({@code index_patterns} +
     * {@code template} wrapper) intended for {@code PUT /_index_template/{name}}. Direct index
     * creation ({@code PUT /{index}}) expects {@code settings}/{@code mappings} at the top level,
     * so the inner {@code template} node is extracted. Alias management is handled separately by
     * {@link #switchAlias(Long)} / {@link #rollback(Long)}, so {@code aliases} are excluded to
     * avoid conflicts when the alias already points to another index.
     */
    private String indexCreationBody(String renderedTemplate) {
        try {
            JsonNode root = objectMapper.readTree(renderedTemplate);
            JsonNode templateNode = root.path("template");
            if (templateNode.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode body = objectMapper.createObjectNode();
                if (templateNode.has("settings")) {
                    body.set("settings", templateNode.get("settings"));
                }
                if (templateNode.has("mappings")) {
                    body.set("mappings", templateNode.get("mappings"));
                }
                return objectMapper.writeValueAsString(body);
            }
            return renderedTemplate;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract index creation body from keyword index template", ex);
        }
    }

    private void backfill(RagKeywordReindexJob job) {
        long total = countChunks(job.getTenantId());
        job.setTotalCount(total);
        jobMapper.updateById(job);
        long success = 0;
        int batchSize = Math.max(1, properties.getKeywordReindex().getBatchSize());
        long offset = 0;
        while (offset < total) {
            List<RagDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<RagDocumentChunk>()
                    .eq(RagDocumentChunk::getTenantId, job.getTenantId())
                    .eq(RagDocumentChunk::getCurrentFlag, true)
                    .eq(RagDocumentChunk::getIsDeleted, 0)
                    .orderByAsc(RagDocumentChunk::getId)
                    .last("LIMIT " + offset + ", " + batchSize));
            if (chunks.isEmpty()) {
                break;
            }
            for (RagDocumentChunk chunk : chunks) {
                request("PUT", indexUri(job.getTargetIndex(), "_doc/" + pathSegment(chunk.getChunkUid())),
                        toJson(documentFor(chunk)), false);
                success++;
            }
            offset += chunks.size();
            int progress = total == 0 ? 80 : (int) Math.min(89, 10 + success * 80 / total);
            job.setSuccessCount(success);
            job.setProgress(progress);
            job.setUpdatedAt(LocalDateTime.now());
            jobMapper.updateById(job);
        }
    }

    private void validate(RagKeywordReindexJob job) {
        // Force a refresh so the _count query sees all documents just indexed by backfill().
        request("POST", indexUri(job.getTargetIndex(), "_refresh"), null, false);
        long sourceCount = countChunks(job.getTenantId());
        long targetCount = countIndex(job.getTargetIndex(), job.getTenantId());

        // KB-filter sample validation: pick one KB and compare MySQL vs ES count
        KbValidation kbVal = validateKbFilter(job.getTargetIndex(), job.getTenantId());

        // Key query sample: run a match_all with tenant filter and verify hits > 0
        boolean keyQueryOk = validateKeyQuery(job.getTargetIndex(), job.getTenantId());

        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("sourceCount", sourceCount);
            result.put("targetCount", targetCount);
            result.put("tenantFilter", true);
            ObjectNode kbFilter = objectMapper.createObjectNode();
            kbFilter.put("knowledgeBaseId", kbVal.kbId());
            kbFilter.put("sourceCount", kbVal.sourceCount());
            kbFilter.put("targetCount", kbVal.targetCount());
            kbFilter.put("passed", kbVal.targetCount() >= kbVal.sourceCount());
            result.set("kbFilter", kbFilter);
            ObjectNode keyQuery = objectMapper.createObjectNode();
            keyQuery.put("passed", keyQueryOk);
            result.set("keyQuery", keyQuery);
            job.setSampleValidationJson(objectMapper.writeValueAsString(result));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to build validation result JSON", e);
        }
        job.setFailedCount(Math.max(0, sourceCount - targetCount));
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.updateById(job);
        if (targetCount < sourceCount) {
            throw new IllegalStateException("Keyword reindex validation failed: targetCount=" + targetCount + ", sourceCount=" + sourceCount);
        }
    }

    private record KbValidation(Long kbId, long sourceCount, long targetCount) {}

    private KbValidation validateKbFilter(String index, Long tenantId) {
        // Find the first KB ID that has chunks for this tenant
        RagDocumentChunk sample = chunkMapper.selectOne(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getTenantId, tenantId)
                .eq(RagDocumentChunk::getCurrentFlag, true)
                .eq(RagDocumentChunk::getIsDeleted, 0)
                .isNotNull(RagDocumentChunk::getKnowledgeBaseId)
                .last("LIMIT 1"));
        if (sample == null || sample.getKnowledgeBaseId() == null) {
            return new KbValidation(0L, 0, 0);
        }
        Long kbId = sample.getKnowledgeBaseId();
        Long sourceCount = chunkMapper.selectCount(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getTenantId, tenantId)
                .eq(RagDocumentChunk::getKnowledgeBaseId, kbId)
                .eq(RagDocumentChunk::getCurrentFlag, true)
                .eq(RagDocumentChunk::getIsDeleted, 0));
        long targetCount = countIndexWithKbFilter(index, tenantId, kbId);
        return new KbValidation(kbId, sourceCount == null ? 0 : sourceCount, targetCount);
    }

    private long countIndexWithKbFilter(String index, Long tenantId, Long kbId) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode query = objectMapper.createObjectNode();
            ObjectNode bool = objectMapper.createObjectNode();
            ArrayNode filter = objectMapper.createArrayNode();
            
            ObjectNode tenantTerm = objectMapper.createObjectNode();
            ObjectNode tenantTermValue = objectMapper.createObjectNode();
            tenantTermValue.put("tenantId", tenantId);
            tenantTerm.set("term", tenantTermValue);
            filter.add(tenantTerm);
            
            ObjectNode kbTerm = objectMapper.createObjectNode();
            ObjectNode kbTermValue = objectMapper.createObjectNode();
            kbTermValue.put("knowledgeBaseId", kbId);
            kbTerm.set("term", kbTermValue);
            filter.add(kbTerm);
            
            bool.set("filter", filter);
            query.set("bool", bool);
            root.set("query", query);
            
            String body = request("POST", indexUri(index, "_count"), objectMapper.writeValueAsString(root), true);
            JsonNode responseRoot = objectMapper.readTree(body);
            return responseRoot.path("count").asLong(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse KB filter count response", ex);
        }
    }

    private boolean validateKeyQuery(String index, Long tenantId) {
        try {
            // Run a simple match query with tenant filter to verify searchability
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode query = objectMapper.createObjectNode();
            ObjectNode bool = objectMapper.createObjectNode();
            ArrayNode filter = objectMapper.createArrayNode();
            ObjectNode term = objectMapper.createObjectNode();
            ObjectNode tenantIdTerm = objectMapper.createObjectNode();
            tenantIdTerm.put("tenantId", tenantId);
            term.set("term", tenantIdTerm);
            filter.add(term);
            bool.set("filter", filter);
            query.set("bool", bool);
            root.set("query", query);
            root.put("size", 1);
            root.put("_source", false);
            
            String body = request("POST", indexUri(index, "_search"), objectMapper.writeValueAsString(root), true);
            JsonNode responseRoot = objectMapper.readTree(body);
            long hits = responseRoot.path("hits").path("total").path("value").asLong(0);
            return hits > 0;
        } catch (IOException ex) {
            log.warn("Key query validation failed for index {}: {}", index, ex.getMessage());
            return false;
        }
    }

    private long countChunks(Long tenantId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getTenantId, tenantId)
                .eq(RagDocumentChunk::getCurrentFlag, true)
                .eq(RagDocumentChunk::getIsDeleted, 0));
        return count == null ? 0 : count;
    }

    private long countIndex(String index, Long tenantId) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode query = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            ObjectNode tenantIdTerm = objectMapper.createObjectNode();
            tenantIdTerm.put("tenantId", tenantId);
            term.set("term", tenantIdTerm);
            query.set("term", term);
            root.set("query", query);
            
            String body = request("POST", indexUri(index, "_count"), objectMapper.writeValueAsString(root), true);
            JsonNode responseRoot = objectMapper.readTree(body);
            return responseRoot.path("count").asLong(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse keyword index count response", ex);
        }
    }

    private RagKeywordReindexJob required(Long jobId) {
        RagKeywordReindexJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Keyword reindex job not found: " + jobId);
        }
        return job;
    }

    private void mark(RagKeywordReindexJob job, String status, Integer progress, String error) {
        String currentStatus = job.getJobStatus();
        if (currentStatus != null) {
            stateMachine.assertTransition(
                    ReindexStateMachine.ReindexJobStatus.valueOf(currentStatus),
                    ReindexStateMachine.ReindexJobStatus.valueOf(status));
        }
        job.setJobStatus(status);
        job.setProgress(progress);
        job.setErrorMessage(error);
        job.setStartedAt(job.getStartedAt() == null ? LocalDateTime.now() : job.getStartedAt());
        job.setUpdatedAt(LocalDateTime.now());
        if ("FAILED".equals(status) || "SUCCEEDED".equals(status) || "ROLLED_BACK".equals(status)) {
            job.setFinishedAt(LocalDateTime.now());
        }
        jobMapper.updateById(job);
    }

    private Map<String, Object> documentFor(RagDocumentChunk chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("tenantId", chunk.getTenantId());
        doc.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
        doc.put("documentDbId", chunk.getDocumentId());
        doc.put("documentId", valueOrDefault(chunk.getSourceDocumentId(), String.valueOf(chunk.getDocumentId())));
        doc.put("documentVersionId", chunk.getDocumentVersionId());
        doc.put("documentName", chunk.getFileName());
        doc.put("chunkId", chunk.getChunkUid());
        doc.put("version", chunk.getChunkVersion());
        doc.put("chunkStatus", chunk.getChunkStatus());
        doc.put("current", Boolean.TRUE.equals(chunk.getCurrentFlag()));
        doc.put("contentType", chunk.getContentType());
        doc.put("title", chunk.getTitle());
        doc.put("sectionPath", chunk.getSectionPath());
        doc.put("content", chunk.getContent());
        doc.put("contentSummary", chunk.getContentSummary());
        doc.put("pageNo", chunk.getPageStart());
        doc.put("permissionTags", chunk.getPermissionTags());
        doc.put("updatedAt", chunk.getUpdatedAt() == null ? null : chunk.getUpdatedAt().toString());
        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize keyword index document", ex);
        }
    }

    private String request(String method, URI uri, String body, boolean allowNotFound) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .header("Content-Type", "application/json");
            headers().forEach(builder::header);
            builder.method(method, StringUtils.hasText(body)
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody());
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404 && allowNotFound) {
                return "{}";
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + ": " + LogMasker.truncateAndMask(response.body()));
            }
            return response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("Keyword index request failed: " + uri + " - " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Keyword index request interrupted", ex);
        }
    }

    private Map<String, String> headers() {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        Map<String, String> headers = new LinkedHashMap<>();
        if (StringUtils.hasText(config.getApiKey())) {
            headers.put("Authorization", "ApiKey " + config.getApiKey());
        } else if (StringUtils.hasText(config.getUsername()) && StringUtils.hasText(config.getPassword())) {
            String credentials = config.getUsername() + ":" + config.getPassword();
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private URI indexUri(String index, String suffix) {
        String path = trimTrailingSlash(properties.getKeywordIndex().getBaseUrl()) + "/" + pathSegment(index);
        if (StringUtils.hasText(suffix)) {
            path += "/" + suffix;
        }
        return URI.create(path);
    }

    private String activeAlias() {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        return StringUtils.hasText(config.getIndexAlias()) ? config.getIndexAlias() : config.getIndexName();
    }

    private String activeIndex() {
        return properties.getKeywordIndex().getIndexName();
    }

    private String targetIndexName(Long tenantId) {
        return activeIndex() + "_tenant_" + tenantId + "_" + System.currentTimeMillis();
    }

    private int timeoutSeconds() {
        Integer seconds = properties.getKeywordIndex().getTimeoutSeconds();
        return seconds == null || seconds < 1 ? 10 : seconds;
    }

    private String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

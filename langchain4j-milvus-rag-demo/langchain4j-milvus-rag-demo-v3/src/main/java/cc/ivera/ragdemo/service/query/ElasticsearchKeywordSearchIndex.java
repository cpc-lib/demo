package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.model.knowledge.ChunkStatus;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ElasticsearchKeywordSearchIndex implements KeywordSearchIndex {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKeywordSearchIndex.class);

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final HttpTransport httpTransport;
    private final KeywordIndexQueryBuilder queryBuilder;
    private final AtomicBoolean indexChecked = new AtomicBoolean(false);

    @Autowired
    public ElasticsearchKeywordSearchIndex(RagProperties ragProperties, ObjectMapper objectMapper) {
        this(ragProperties, objectMapper, new JdkHttpTransport(), new KeywordIndexQueryBuilder());
    }

    ElasticsearchKeywordSearchIndex(RagProperties ragProperties, ObjectMapper objectMapper, HttpTransport httpTransport) {
        this(ragProperties, objectMapper, httpTransport, new KeywordIndexQueryBuilder());
    }

    ElasticsearchKeywordSearchIndex(RagProperties ragProperties,
                                    ObjectMapper objectMapper,
                                    HttpTransport httpTransport,
                                    KeywordIndexQueryBuilder queryBuilder) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpTransport = httpTransport;
        this.queryBuilder = queryBuilder;
    }

    @Override
    public boolean enabled() {
        RagProperties.KeywordIndex keywordIndex = ragProperties.getKeywordIndex();
        return keywordIndex.isEnabled()
                && "elasticsearch".equalsIgnoreCase(keywordIndex.getProvider())
                && StringUtils.hasText(keywordIndex.getBaseUrl())
                && StringUtils.hasText(keywordIndex.getIndexName());
    }

    @Override
    public List<RagSearchItem> search(RagRetrievalCriteria criteria, int candidateLimit) {
        if (!enabled() || criteria == null || !StringUtils.hasText(criteria.query())) {
            return List.of();
        }
        try {
            HttpResult result = request(
                    "POST",
                    indexUri("_search"),
                    objectMapper.writeValueAsString(queryBuilder.searchPayload(criteria, candidateLimit)),
                    true
            );
            if (result.statusCode() == 404) {
                return List.of();
            }
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                throw new IOException("Elasticsearch search failed: HTTP " + result.statusCode());
            }
            return parseSearchResults(result.body());
        } catch (Exception e) {
            log.warn("Elasticsearch keyword search failed, fallback to MySQL keyword retrieval: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void upsert(RagDocumentChunk chunk) {
        if (!enabled() || chunk == null || !StringUtils.hasText(chunk.getChunkUid())) {
            return;
        }
        if (!Boolean.TRUE.equals(chunk.getCurrentFlag())
                || !"ACTIVE".equalsIgnoreCase(chunk.getChunkStatus())
                || Integer.valueOf(1).equals(chunk.getIsDeleted())) {
            delete(chunk.getChunkUid());
            return;
        }
        try {
            ensureIndex();
            request("PUT", indexUri("_doc/" + pathSegment(chunk.getChunkUid())),
                    objectMapper.writeValueAsString(documentFor(chunk)), false);
        } catch (Exception e) {
            log.warn("Failed to upsert Elasticsearch chunk index for {}: {}", chunk.getChunkUid(), e.getMessage());
        }
    }

    @Override
    public void upsert(KnowledgeChunkRecord record) {
        if (!enabled() || record == null || !StringUtils.hasText(record.chunkId())) {
            return;
        }
        if (!record.current() || record.status() != ChunkStatus.ACTIVE) {
            delete(record.chunkId());
            return;
        }
        try {
            ensureIndex();
            request("PUT", indexUri("_doc/" + pathSegment(record.chunkId())),
                    objectMapper.writeValueAsString(documentFor(record)), false);
        } catch (Exception e) {
            log.warn("Failed to upsert Elasticsearch chunk index for {}: {}", record.chunkId(), e.getMessage());
        }
    }

    @Override
    public void delete(String chunkId) {
        if (!enabled() || !StringUtils.hasText(chunkId)) {
            return;
        }
        try {
            request("DELETE", indexUri("_doc/" + pathSegment(chunkId)), null, true);
        } catch (Exception e) {
            log.warn("Failed to delete Elasticsearch chunk index for {}: {}", chunkId, e.getMessage());
        }
    }

    private List<RagSearchItem> parseSearchResults(String body) throws IOException {
        JsonNode hits = objectMapper.readTree(body).path("hits").path("hits");
        if (!hits.isArray()) {
            return List.of();
        }
        List<RagSearchItem> items = new ArrayList<>();
        int rank = 1;
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            Map<String, Object> metadata = metadata(source);
            metadata.put("keyword_algorithm", "elasticsearch_bm25");
            metadata.put("keyword_index_provider", "elasticsearch");
            metadata.put("keyword_index_name", ragProperties.getKeywordIndex().getIndexName());
            double score = hit.path("_score").asDouble(0.0);
            items.add(new RagSearchItem(
                    rank++,
                    score,
                    longValue(source.path("knowledgeBaseId")),
                    text(source.path("documentId")),
                    text(source.path("documentName")),
                    text(source.path("chunkId")),
                    intValue(source.path("version")),
                    text(source.path("contentType")),
                    intValue(source.path("pageNo")),
                    firstNonBlank(text(source.path("sectionPath")), text(source.path("title"))),
                    text(source.path("imageCaption")),
                    text(source.path("imageNumber")),
                    text(source.path("imageUrl")),
                    text(source.path("content")),
                    metadata
            ));
        }
        return items;
    }

    private Map<String, Object> documentFor(RagDocumentChunk chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("tenantId", chunk.getTenantId() == null ? 0L : chunk.getTenantId());
        doc.put("tenant_id", chunk.getTenantId() == null ? 0L : chunk.getTenantId());
        doc.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
        doc.put("knowledge_base_id", chunk.getKnowledgeBaseId());
        doc.put("documentDbId", chunk.getDocumentId());
        doc.put("documentId", firstNonBlank(chunk.getSourceDocumentId(), stringValue(chunk.getDocumentId())));
        doc.put("document_id", firstNonBlank(chunk.getSourceDocumentId(), stringValue(chunk.getDocumentId())));
        doc.put("documentVersionId", chunk.getDocumentVersionId());
        doc.put("documentName", chunk.getFileName());
        doc.put("document_name", chunk.getFileName());
        doc.put("chunkId", chunk.getChunkUid());
        doc.put("chunk_uid", chunk.getChunkUid());
        doc.put("version", chunk.getChunkVersion());
        doc.put("chunkStatus", chunk.getChunkStatus());
        doc.put("current", Boolean.TRUE.equals(chunk.getCurrentFlag()));
        doc.put("is_current", Boolean.TRUE.equals(chunk.getCurrentFlag()));
        doc.put("contentType", chunk.getContentType());
        doc.put("content_type", chunk.getContentType());
        doc.put("title", chunk.getTitle());
        doc.put("sectionPath", chunk.getSectionPath());
        doc.put("section_path", chunk.getSectionPath());
        doc.put("content", chunk.getContent());
        doc.put("contentSummary", chunk.getContentSummary());
        doc.put("pageNo", chunk.getPageStart());
        doc.put("imageUrl", chunk.getImageUrl());
        doc.put("imageCaption", chunk.getImageCaption());
        doc.put("image_caption", chunk.getImageCaption());
        doc.put("imageNumber", chunk.getImageNumber());
        doc.put("permissionTags", normalizedTagList(chunk.getPermissionTags()));
        doc.put("permission_tags", normalizedTagList(chunk.getPermissionTags()));
        doc.put("vectorId", chunk.getVectorId());
        doc.put("metadataJson", chunk.getMetadataJson());
        doc.put("updatedAt", chunk.getUpdatedAt() == null ? null : chunk.getUpdatedAt().toString());
        doc.put("updated_at", chunk.getUpdatedAt() == null ? null : chunk.getUpdatedAt().toString());
        doc.put("isDeleted", Integer.valueOf(1).equals(chunk.getIsDeleted()));
        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }

    private Map<String, Object> documentFor(KnowledgeChunkRecord record) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("tenantId", parseLongOrDefault(record.tenantId(), 0L));
        doc.put("tenant_id", parseLongOrDefault(record.tenantId(), 0L));
        doc.put("knowledgeBaseId", 0L);
        doc.put("knowledge_base_id", 0L);
        doc.put("documentId", record.documentId());
        doc.put("document_id", record.documentId());
        doc.put("documentName", record.fileName());
        doc.put("document_name", record.fileName());
        doc.put("chunkId", record.chunkId());
        doc.put("chunk_uid", record.chunkId());
        doc.put("version", record.version());
        doc.put("chunkStatus", record.status() == null ? "ACTIVE" : record.status().name());
        doc.put("current", record.current());
        doc.put("is_current", record.current());
        doc.put("contentType", record.contentType());
        doc.put("content_type", record.contentType());
        doc.put("title", record.sectionTitle());
        doc.put("sectionPath", record.sectionTitle());
        doc.put("section_path", record.sectionTitle());
        doc.put("content", record.textContent());
        doc.put("contentSummary", truncate(record.textContent(), 500));
        doc.put("pageNo", record.pageNo());
        doc.put("imageUrl", record.imageUrl());
        doc.put("imageCaption", record.imageCaption());
        doc.put("image_caption", record.imageCaption());
        doc.put("imageNumber", record.imageNumber());
        doc.put("permissionTags", record.permissionTags() == null ? List.of() : record.permissionTags());
        doc.put("permission_tags", record.permissionTags() == null ? List.of() : record.permissionTags());
        doc.put("vectorId", record.textVectorIds() == null || record.textVectorIds().isEmpty() ? null : record.textVectorIds().get(0));
        doc.put("metadataJson", record.metadataJson());
        doc.put("updatedAt", record.updatedAt() == null ? LocalDateTime.now().toString() : record.updatedAt().toString());
        doc.put("updated_at", record.updatedAt() == null ? LocalDateTime.now().toString() : record.updatedAt().toString());
        doc.put("isDeleted", false);
        doc.entrySet().removeIf(entry -> entry.getValue() == null);
        return doc;
    }

    private Map<String, Object> metadata(JsonNode source) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retrieval_mode", "keyword");
        metadata.put("tenant_id", stringValue(source.path("tenantId")));
        metadata.put("knowledge_base_id", stringValue(source.path("knowledgeBaseId")));
        metadata.put("document_id", text(source.path("documentId")));
        metadata.put("fileName", text(source.path("documentName")));
        metadata.put("chunk_id", text(source.path("chunkId")));
        metadata.put("version", intValue(source.path("version")));
        metadata.put("content_type", text(source.path("contentType")));
        metadata.put("page_no", intValue(source.path("pageNo")));
        metadata.put("section_title", firstNonBlank(text(source.path("sectionPath")), text(source.path("title"))));
        metadata.put("image_caption", text(source.path("imageCaption")));
        metadata.put("image_number", text(source.path("imageNumber")));
        metadata.put("image_url", text(source.path("imageUrl")));
        metadata.put("permission_tags", textOrJoined(source.path("permissionTags")));
        metadata.put("current", String.valueOf(source.path("current").asBoolean(false)));
        metadata.put("chunk_status", text(source.path("chunkStatus")));
        metadata.put("vector_id", text(source.path("vectorId")));
        metadata.put("metadata_json", text(source.path("metadataJson")));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return metadata;
    }

    private void ensureIndex() throws IOException, InterruptedException {
        if (!ragProperties.getKeywordIndex().isAutoCreateIndex() || !indexChecked.compareAndSet(false, true)) {
            return;
        }
        HttpResult head = request("HEAD", indexUri(null), null, true);
        if (head.statusCode() != 404) {
            return;
        }
        request("PUT", indexUri(null), objectMapper.writeValueAsString(indexMapping()), false);
    }

    private Map<String, Object> indexMapping() {
        Map<String, Object> textWithKeyword = Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword")));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("tenantId", Map.of("type", "long"));
        properties.put("knowledgeBaseId", Map.of("type", "long"));
        properties.put("documentDbId", Map.of("type", "long"));
        properties.put("documentVersionId", Map.of("type", "long"));
        properties.put("documentId", Map.of("type", "keyword"));
        properties.put("documentName", textWithKeyword);
        properties.put("chunkId", Map.of("type", "keyword"));
        properties.put("version", Map.of("type", "integer"));
        properties.put("chunkStatus", Map.of("type", "keyword"));
        properties.put("current", Map.of("type", "boolean"));
        properties.put("contentType", Map.of("type", "keyword"));
        properties.put("title", textWithKeyword);
        properties.put("sectionPath", textWithKeyword);
        properties.put("contentSummary", Map.of("type", "text"));
        properties.put("content", Map.of("type", "text"));
        properties.put("pageNo", Map.of("type", "integer"));
        properties.put("permissionTags", Map.of("type", "keyword"));
        properties.put("updatedAt", Map.of("type", "date", "ignore_malformed", true));
        properties.put("isDeleted", Map.of("type", "boolean"));
        return Map.of("mappings", Map.of("properties", properties));
    }

    private HttpResult request(String method, URI uri, String body, boolean allowNotFound) throws IOException, InterruptedException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        applyAuth(headers);
        HttpResult result = httpTransport.request(method, uri, headers, body, timeout());
        if (!allowNotFound && (result.statusCode() < 200 || result.statusCode() >= 300)) {
            throw new IOException("Elasticsearch request failed: HTTP " + result.statusCode());
        }
        return result;
    }

    private void applyAuth(Map<String, String> headers) {
        RagProperties.KeywordIndex keywordIndex = ragProperties.getKeywordIndex();
        if (StringUtils.hasText(keywordIndex.getApiKey())) {
            headers.put("Authorization", "ApiKey " + keywordIndex.getApiKey());
            return;
        }
        if (StringUtils.hasText(keywordIndex.getUsername()) && StringUtils.hasText(keywordIndex.getPassword())) {
            String credentials = keywordIndex.getUsername() + ":" + keywordIndex.getPassword();
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private URI indexUri(String suffix) {
        RagProperties.KeywordIndex keywordIndex = ragProperties.getKeywordIndex();
        String base = trimTrailingSlash(keywordIndex.getBaseUrl());
        String path = pathSegment(keywordIndex.getIndexName());
        if (StringUtils.hasText(suffix)) {
            path += "/" + suffix;
        }
        return URI.create(base + "/" + path);
    }

    private Duration timeout() {
        Integer seconds = ragProperties.getKeywordIndex().getTimeoutSeconds();
        return Duration.ofSeconds(seconds == null || seconds < 1 ? 10 : seconds);
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private Map<String, Object> terms(String field, Object value) {
        return Map.of("terms", Map.of(field, value));
    }

    private List<String> normalizedTagList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split("[,;\\s]+")).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private String textOrJoined(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                if (item != null && !item.isNull() && StringUtils.hasText(item.asText())) {
                    values.add(item.asText());
                }
            }
            return String.join(",", values);
        }
        return node.asText();
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.canConvertToLong() ? node.asLong() : null;
    }

    private Integer intValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.canConvertToInt() ? node.asInt() : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return text(node);
        }
        return String.valueOf(value);
    }

    private Long parseLongOrDefault(String value, Long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    record HttpResult(int statusCode, String body) {
    }

    interface HttpTransport {
        HttpResult request(String method,
                           URI uri,
                           Map<String, String> headers,
                           String body,
                           Duration timeout) throws IOException, InterruptedException;
    }

    private static class JdkHttpTransport implements HttpTransport {
        private final HttpClient httpClient = HttpClient.newHttpClient();

        @Override
        public HttpResult request(String method,
                                  URI uri,
                                  Map<String, String> headers,
                                  String body,
                                  Duration timeout) throws IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);
            headers.forEach(builder::header);
            HttpRequest.BodyPublisher publisher = StringUtils.hasText(body)
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody();
            builder.method(method, publisher);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        }
    }
}

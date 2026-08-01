package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
public class KeywordIndexQueryBuilder {

    public Map<String, Object> searchPayload(RagRetrievalCriteria criteria, int candidateLimit) {
        Map<String, Object> multiMatch = new LinkedHashMap<>();
        multiMatch.put("query", criteria.query().trim());
        multiMatch.put("fields", searchFields());
        multiMatch.put("type", "best_fields");
        multiMatch.put("operator", "or");

        List<Object> filters = new ArrayList<>();
        Long tenantId = TenantContextHolder.currentTenantId().orElse(criteria.tenantId() == null ? 0L : criteria.tenantId());
        filters.add(term("tenantId", tenantId));
        filters.add(term("current", true));
        filters.add(term("chunkStatus", "ACTIVE"));
        filters.add(term("isDeleted", false));
        if (criteria.knowledgeBaseIds() != null && !criteria.knowledgeBaseIds().isEmpty()) {
            filters.add(terms("knowledgeBaseId", criteria.knowledgeBaseIds()));
        }
        List<String> contentTypes = normalizedStrings(criteria.contentTypes());
        if (!contentTypes.isEmpty()) {
            filters.add(terms("contentType", contentTypes));
        }
        List<String> permissionTags = normalizedStrings(criteria.permissionTags());
        if (!permissionTags.isEmpty()) {
            filters.add(terms("permissionTags", permissionTags));
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", List.of(Map.of("multi_match", multiMatch)));
        bool.put("filter", filters);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("size", Math.max(1, candidateLimit));
        payload.put("query", Map.of("bool", bool));
        payload.put("_source", true);
        return payload;
    }

    public List<String> searchFields() {
        return List.of(
                "title^3",
                "sectionPath^2",
                "section_path^2",
                "contentSummary^2",
                "documentName^1.5",
                "document_name^1.5",
                "content",
                "content.smart^1.2",
                "content.en",
                "content.synonym^1.5",
                "imageCaption^1.2",
                "image_caption^1.2",
                "ocr_text"
        );
    }

    private Map<String, Object> term(String field, Object value) {
        return Map.of("term", Map.of(field, value));
    }

    private Map<String, Object> terms(String field, Object value) {
        return Map.of("terms", Map.of(field, value));
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
}

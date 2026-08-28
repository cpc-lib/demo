package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import dev.langchain4j.store.embedding.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Component
public class RetrievalFilterBuilder {

    @Autowired
    public RetrievalFilterBuilder() {
    }

    public Optional<Filter> buildMilvusFilter(RagRetrievalCriteria criteria) {
        List<Filter> filters = new ArrayList<>();

        Long tenantId = TenantContextHolder.currentTenantId().orElse(criteria.tenantId() == null ? 0L : criteria.tenantId());
        filters.add(metadataKey("tenant_id").isEqualTo(String.valueOf(tenantId)));

        List<String> knowledgeBaseIds = stringIds(criteria.knowledgeBaseIds());
        if (!knowledgeBaseIds.isEmpty()) {
            filters.add(inOrEquals("knowledge_base_id", knowledgeBaseIds));
        }

        filters.add(metadataKey("current").isEqualTo("true"));
        filters.add(metadataKey("chunk_status").isEqualTo("ACTIVE"));

        List<String> contentTypes = normalizedStrings(criteria.contentTypes());
        if (!contentTypes.isEmpty()) {
            filters.add(inOrEquals("content_type", contentTypes));
        }

        return andAll(filters);
    }

    public boolean permissionsMatch(Map<String, Object> metadata, List<String> requiredPermissionTags) {
        Set<String> required = new LinkedHashSet<>(normalizedStrings(requiredPermissionTags));
        if (required.isEmpty()) {
            return true;
        }

        Set<String> available = permissionTags(metadata == null ? null : metadata.get("permission_tags"));
        if (available.isEmpty()) {
            return false;
        }
        return required.stream().anyMatch(available::contains);
    }

    private Optional<Filter> andAll(List<Filter> filters) {
        if (filters.isEmpty()) {
            return Optional.empty();
        }
        Filter current = filters.get(0);
        for (int i = 1; i < filters.size(); i++) {
            current = current.and(filters.get(i));
        }
        return Optional.of(current);
    }

    private Filter inOrEquals(String key, List<String> values) {
        if (values.size() == 1) {
            return metadataKey(key).isEqualTo(values.get(0));
        }
        return metadataKey(key).isIn(values);
    }

    private List<String> stringIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
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

    private Set<String> permissionTags(Object rawValue) {
        if (rawValue == null) {
            return Set.of();
        }
        if (rawValue instanceof Collection<?> collection) {
            Set<String> tags = new LinkedHashSet<>();
            for (Object value : collection) {
                addTag(tags, value);
            }
            return tags;
        }
        String raw = String.valueOf(rawValue)
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");
        Set<String> tags = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .forEach(value -> addTag(tags, value));
        return tags;
    }

    private void addTag(Set<String> tags, Object value) {
        if (value == null) {
            return;
        }
        String tag = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (!tag.isBlank()) {
            tags.add(tag);
        }
    }
}

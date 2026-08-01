package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.model.query.RagSearchItem;
import org.springframework.util.StringUtils;

import java.util.*;

public class CrossTenantGuardPolicy {

    public boolean metadataMatchesTenant(Map<String, Object> metadata, Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        Object value = metadata == null ? null : metadata.get("tenant_id");
        return value != null && String.valueOf(tenantId).equals(String.valueOf(value));
    }

    public boolean itemAllowed(RagSearchItem item,
                               Long tenantId,
                               Collection<Long> authorizedKnowledgeBaseIds,
                               Collection<String> requiredPermissionTags) {
        if (item == null || !metadataMatchesTenant(item.metadata(), tenantId)) {
            return false;
        }
        Set<Long> authorized = new LinkedHashSet<>(authorizedKnowledgeBaseIds == null ? List.of() : authorizedKnowledgeBaseIds);
        if (!authorized.isEmpty() && (item.knowledgeBaseId() == null || !authorized.contains(item.knowledgeBaseId()))) {
            return false;
        }
        Set<String> required = normalizeTags(requiredPermissionTags);
        if (required.isEmpty()) {
            return true;
        }
        Set<String> actual = permissionTags(item.metadata() == null ? null : item.metadata().get("permission_tags"));
        return required.stream().anyMatch(actual::contains);
    }

    public List<RagSearchItem> filterAllowed(List<RagSearchItem> items,
                                             Long tenantId,
                                             Collection<Long> authorizedKnowledgeBaseIds,
                                             Collection<String> requiredPermissionTags) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> itemAllowed(item, tenantId, authorizedKnowledgeBaseIds, requiredPermissionTags))
                .toList();
    }

    private Set<String> normalizeTags(Collection<String> values) {
        Set<String> tags = new LinkedHashSet<>();
        if (values == null) {
            return tags;
        }
        values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase())
                .forEach(tags::add);
        return tags;
    }

    private Set<String> permissionTags(Object value) {
        Set<String> tags = new LinkedHashSet<>();
        if (value == null) {
            return tags;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    tags.add(String.valueOf(item).trim().toLowerCase());
                }
            });
            return tags;
        }
        for (String raw : String.valueOf(value).replace("[", "").replace("]", "").replace("\"", "").split(",")) {
            if (StringUtils.hasText(raw)) {
                tags.add(raw.trim().toLowerCase());
            }
        }
        return tags;
    }
}

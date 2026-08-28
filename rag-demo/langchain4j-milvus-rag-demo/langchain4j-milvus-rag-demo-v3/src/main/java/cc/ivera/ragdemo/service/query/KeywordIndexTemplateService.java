package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.KeywordIndexAliasSwitchPlan;
import cc.ivera.ragdemo.model.query.KeywordIndexTemplateDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KeywordIndexTemplateService {

    private static final String TEMPLATE_RESOURCE = "elasticsearch/templates/rag_chunks_zh_en_v2.json";

    private static final List<String> PROFILES = List.of(
            "standard_zh_en",
            "ik_zh_en",
            "smartcn_zh_en",
            "business_synonym",
            "exact_keyword_boost"
    );

    private final RagProperties ragProperties;

    public List<String> analyzerProfiles() {
        return PROFILES;
    }

    public KeywordIndexTemplateDescriptor currentTemplate() {
        RagProperties.KeywordIndex config = ragProperties.getKeywordIndex();
        String profile = normalizeProfile(config.getAnalyzerProfile());
        return new KeywordIndexTemplateDescriptor(
                "elasticsearch",
                profile,
                templateName(config),
                TEMPLATE_RESOURCE,
                render(TEMPLATE_RESOURCE)
        );
    }

    public String render(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Keyword index template not found: " + resourcePath);
            }
            String json = resource.getContentAsString(StandardCharsets.UTF_8);
            RagProperties.KeywordIndex config = ragProperties.getKeywordIndex();
            return json
                    .replace("${index_name}", config.getIndexName())
                    .replace("${index_alias}", activeAlias())
                    .replace("${index_version}", config.getIndexVersion())
                    .replace("${analyzer_profile}", normalizeProfile(config.getAnalyzerProfile()))
                    .replace("${synonym_path}", config.getSynonymPath())
                    .replace("${stopword_path}", config.getStopwordPath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read keyword index template: " + resourcePath, e);
        }
    }

    public KeywordIndexAliasSwitchPlan aliasSwitchPlan(String fromIndex, String toIndex) {
        if (!StringUtils.hasText(toIndex)) {
            throw new IllegalArgumentException("Target index is required");
        }
        String alias = activeAlias();
        StringBuilder actions = new StringBuilder();
        actions.append("{\"actions\":[");
        if (StringUtils.hasText(fromIndex)) {
            actions.append("{\"remove\":{\"index\":\"")
                    .append(escape(fromIndex.trim()))
                    .append("\",\"alias\":\"")
                    .append(escape(alias))
                    .append("\"}},");
        }
        actions.append("{\"add\":{\"index\":\"")
                .append(escape(toIndex.trim()))
                .append("\",\"alias\":\"")
                .append(escape(alias))
                .append("\"}}]}");
        return new KeywordIndexAliasSwitchPlan(
                alias,
                fromIndex,
                toIndex.trim(),
                "elasticsearch",
                "/_aliases",
                actions.toString()
        );
    }

    private String templateName(RagProperties.KeywordIndex config) {
        return config.getIndexName() + "_" + normalizeProfile(config.getAnalyzerProfile()) + "_" + config.getIndexVersion();
    }

    private String activeAlias() {
        RagProperties.KeywordIndex config = ragProperties.getKeywordIndex();
        return StringUtils.hasText(config.getIndexAlias()) ? config.getIndexAlias() : config.getIndexName();
    }

    private String normalizeProfile(String profile) {
        if (!StringUtils.hasText(profile)) {
            return "standard_zh_en";
        }
        String normalized = profile.trim().toLowerCase();
        return PROFILES.contains(normalized) ? normalized : "standard_zh_en";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

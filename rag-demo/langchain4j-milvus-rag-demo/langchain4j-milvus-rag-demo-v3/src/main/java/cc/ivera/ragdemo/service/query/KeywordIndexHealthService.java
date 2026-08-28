package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.KeywordIndexHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KeywordIndexHealthService {

    private final RagProperties ragProperties;

    public KeywordIndexHealthResponse health() {
        RagProperties.KeywordIndex config = ragProperties.getKeywordIndex();
        boolean usable = config.isEnabled()
                && StringUtils.hasText(config.getProvider())
                && StringUtils.hasText(config.getBaseUrl())
                && StringUtils.hasText(config.getIndexName());
        String target = StringUtils.hasText(config.getIndexAlias()) ? config.getIndexAlias() : config.getIndexName();
        return new KeywordIndexHealthResponse(
                config.isEnabled(),
                config.getProvider(),
                "elasticsearch",
                config.getBaseUrl(),
                config.getIndexName(),
                config.getIndexAlias(),
                config.getIndexVersion(),
                config.getAnalyzerProfile(),
                config.isTemplateManaged(),
                config.isAutoCreateIndex(),
                target,
                usable ? "CONFIGURED" : "DISABLED_OR_INCOMPLETE"
        );
    }
}

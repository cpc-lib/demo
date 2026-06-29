package com.example.vocab.config.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {
    /** mysql | elasticsearch */
    private String provider = "mysql";
    /** When true, saving a word also tries to sync a search document to Elasticsearch. */
    private boolean syncElasticsearch = false;
}

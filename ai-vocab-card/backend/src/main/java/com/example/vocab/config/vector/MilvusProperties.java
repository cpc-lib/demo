package com.example.vocab.config.vector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.milvus")
public class MilvusProperties {
    /** REST gateway endpoint. In production this can point to Milvus Proxy or an internal vector adapter. */
    private String endpoint = "http://localhost:19530";
    private String collection = "word_card_vectors";
    private String token = "";
    private Integer dimension = 1024;
    private Boolean enabled = false;
}

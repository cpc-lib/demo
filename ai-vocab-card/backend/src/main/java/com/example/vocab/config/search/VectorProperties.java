package com.example.vocab.config.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.vector")
public class VectorProperties {
    /** local | milvus-adapter. The local provider is deterministic and keeps the project runnable without Milvus. */
    private String provider = "local";
    private int topK = 10;
}

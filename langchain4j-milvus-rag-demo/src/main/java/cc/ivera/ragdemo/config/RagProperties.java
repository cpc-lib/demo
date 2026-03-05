package cc.ivera.ragdemo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Llm llm = new Llm();
    private final Embedding embedding = new Embedding();
    private final Milvus milvus = new Milvus();
    private final Splitter splitter = new Splitter();

    @Data
    public static class Llm {
        @NotBlank
        private String baseUrl;
        @NotBlank
        private String apiKey;
        @NotBlank
        private String model;
        private double temperature = 0.2;
    }

    @Data
    public static class Embedding {
        @NotBlank
        private String baseUrl;
        @NotBlank
        private String apiKey;
        @NotBlank
        private String model;
        @Min(1)
        private int dimension = 1536;
    }

    @Data
    public static class Milvus {
        @NotBlank
        private String host;
        @Min(1)
        private int port = 19530;
        @NotBlank
        private String collection = "demo_kb";
        @Min(1)
        private int topK = 6;
        private double minScore = 0.55;
    }

    @Data
    public static class Splitter {
        @Min(100)
        private int chunkSize = 900;
        @Min(0)
        private int overlap = 120;
    }
}

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
    private final Agent agent = new Agent();
    private final WebSearch webSearch = new WebSearch();
    private final Weather weather = new Weather();

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
        private String defaultAlias = "default";
        @NotBlank
        private String host;
        @Min(1)
        private int port = 19530;
        @NotBlank
        private String collection = "demo_kb";
        @Min(1)
        private int topK = 6;
        private double minScore = 0.55;
        private String username;
        private String password;
    }

    @Data
    public static class Splitter {
        @Min(100)
        private int chunkSize = 900;
        @Min(0)
        private int overlap = 120;
    }

    @Data
    public static class Agent {
        @Min(2)
        private int memoryMaxMessages = 20;
        private boolean appendSourceBlock = true;
        private boolean appendToolTrace = true;
    }

    @Data
    public static class WebSearch {
        private boolean enabled = true;
        private String provider = "tavily";
        private String tavilyApiKey;
        @Min(1)
        private int maxResults = 10;
        private String searchDepth = "advanced";
        private Integer timeoutSeconds = 20;
    }

    @Data
    public static class Weather {
        private boolean enabled = true;
        private String geocodingBaseUrl = "https://geocoding-api.open-meteo.com";
        private String forecastBaseUrl = "https://api.open-meteo.com";
        private String defaultTimezone = "Asia/Shanghai";
        private Integer timeoutSeconds = 20;
    }
}

package cc.ivera.ragdemo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

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
    private final Image image = new Image();
    private final MultimodalIngest multimodalIngest = new MultimodalIngest();
    private final Ingestion ingestion = new Ingestion();
    private final ChunkRegistry chunkRegistry = new ChunkRegistry();
    private final Retrieval retrieval = new Retrieval();
    private final KeywordIndex keywordIndex = new KeywordIndex();
    private final Tenant tenant = new Tenant();
    private final Security security = new Security();
    private final IngestionEvents ingestionEvents = new IngestionEvents();
    private final TenantDeletion tenantDeletion = new TenantDeletion();
    private final KeywordReindex keywordReindex = new KeywordReindex();
    private final Metrics metrics = new Metrics();
    private final ObjectStorage objectStorage = new ObjectStorage();
    private final Scheduler scheduler = new Scheduler();
    private final Schema schema = new Schema();

    @Data
    public static class Scheduler {
        /** 定时任务线程池大小，默认3（与项目中定时任务数量匹配） */
        @Min(1)
        private int poolSize = 3;
        
        /** 线程池关闭时等待任务完成的秒数 */
        @Min(1)
        private int awaitTerminationSeconds = 60;
    }

    @Data
    public static class Schema {
        /** Whether to create missing local demo tables from the bundled SQL bootstrap. */
        private boolean autoInitialize = false;
        /** SQL bootstrap source used by the local schema initializer. */
        private String bootstrapLocation = "classpath:sql/all-in-one.sql";
    }

    @Data
    public static class Llm {
        private String provider = "openai-compatible";
        private String baseUrl;
        private String apiKey;
        private String model;
        private double temperature = 0.2;
    }

    @Data
    public static class Embedding {
        private String provider = "openai-compatible";
        private String baseUrl;
        private String apiKey;
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
        private boolean multimodalEnabled = false;
        private String multimodalCollection = "rag_chunks_multimodal_v1";
        private String textVectorField = "text_vector";
        private String imageVectorField = "image_vector";
        private String multimodalMetricType = "COSINE";
        private String multimodalIndexType = "HNSW";
        private String multimodalIndexParams = "{\"M\":16,\"efConstruction\":200}";
        private String multimodalSearchParams = "{\"ef\":64}";
        private boolean autoCreateMultimodalCollection = true;

        /** Connection timeout in milliseconds (default: 10000ms). */
        @Min(1000)
        private long connectTimeoutMs = 10000L;

        /** RPC deadline in milliseconds. Keep 0 by default because Milvus SDK 2.3.9 stores this on a long-lived stub. */
        @Min(0)
        private long rpcDeadlineMs = 0L;

        /** Keep-alive time interval in milliseconds (default: 10000ms). */
        @Min(1000)
        private long keepAliveTimeMs = 10000L;

        /** Keep-alive timeout in milliseconds (default: 5000ms). */
        @Min(1000)
        private long keepAliveTimeoutMs = 5000L;

        /** Whether to send keep-alive pings even without active RPCs (default: true). */
        private boolean keepAliveWithoutCalls = true;

        /** Idle timeout in milliseconds before closing connection (default: 86400000ms = 24 hours). */
        @Min(60000)
        private long idleTimeoutMs = 86400000L;

        /** Whether to enable TLS (default: false). */
        private boolean secure = false;

        /** Whether to perform connectivity check before returning client (default: false). */
        private boolean enablePrecheck = false;

        /** Default database name (optional). */
        private String dbName;
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
        private List<String> tools = new ArrayList<>(List.of("ticket", "knowledge", "web-search", "weather", "text-to-image"));
    }

    @Data
    public static class WebSearch {
        private boolean enabled = true;
        private String provider = "tavily";
        private String tavilyBaseUrl = "https://api.tavily.com";
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

    @Data
    public static class Image {
        private boolean enabled = true;
        private String baseUrl;
        private String apiKey;
        private String model = "gpt-image-1";
        private String size = "1024x1024";
        private String quality = "standard";
        private Integer timeoutSeconds = 60;
        private Integer pollIntervalMillis = 2000;
    }

    @Data
    public static class MultimodalIngest {
        private boolean enabled = true;
        private boolean visionAnalysisEnabled = true;
        private String visionBaseUrl;
        private String visionApiKey;
        private String visionModel;
        private boolean ocrEnabled = false;
        private String ocrProvider = "noop";
        private String ocrBaseUrl;
        private String ocrApiKey;
        private String ocrModel;
        private boolean visionEmbeddingEnabled = false;
        private String visionEmbeddingProvider = "dashscope";
        private String visionEmbeddingBaseUrl;
        private String visionEmbeddingApiKey;
        private String visionEmbeddingModel;
        @Min(1)
        private int visionEmbeddingDimension = 1024;
        private double lowConfidenceThreshold = 0.70D;
        private String visualSchemaPath = "classpath:schema/visual-knowledge.schema.json";
        private String ocrSchemaPath = "classpath:schema/ocr-result.schema.json";
        private String assetDirectory = "data/knowledge-assets";
        private String assetUrlPrefix = "/api/knowledge/assets";
        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Ingestion {
        private boolean rabbitEnabled = true;
        private boolean consumerAutoStartup = true;
        private String exchange = "rag.ingestion.exchange";
        private String queueName = "rag.ingestion.tasks";
        private String routingKey = "rag.ingestion.task";
        private String objectDirectory = "data/rag-objects";
        private Long defaultTenantId = 0L;
        private String defaultKnowledgeBaseCode = "default";
        private String defaultKnowledgeBaseName = "Default Knowledge Base";
    }

    @Data
    public static class ChunkRegistry {
        private boolean enabled = true;
        private boolean rebuildOnStartup = true;
        private boolean scheduledRebuildEnabled = false;
        private boolean clearExistingOnRebuild = true;
        @Min(1000)
        private long rebuildInitialDelayMillis = 3_600_000L;
        @Min(1000)
        private long rebuildFixedDelayMillis = 3_600_000L;
    }

    @Data
    public static class Retrieval {
        private double hybridVectorWeight = 0.65;
        private double hybridKeywordWeight = 0.35;
        private double multimodalTextVectorWeight = 0.40;
        private double multimodalImageVectorWeight = 0.40;
        private double multimodalKeywordWeight = 0.20;
        private double imageOnlyVectorWeight = 0.80;
        private double imageOnlyKeywordWeight = 0.20;
        private double rrfK = 60.0;
        private boolean rerankEnabled = false;
        private String rerankProvider = "dashscope";
        private String rerankBaseUrl = "https://dashscope.aliyuncs.com/compatible-api/v1";
        private String rerankApiKey;
        private String rerankModel = "qwen3-rerank";
        private int rerankCandidateLimit = 50;
        private Integer rerankTimeoutSeconds = 20;
        private boolean rerankObservationEnabled = true;
        private double rerankCostPer1kTokens = 0.0;
        private int keywordCandidateMultiplier = 8;
        private String evaluationSetPath = "data/rag-eval/retrieval-eval-set.json";
    }

    @Data
    public static class KeywordIndex {
        private boolean enabled = false;
        private String provider = "elasticsearch";
        private String baseUrl;
        private String indexName = "rag_chunks";
        private String indexAlias = "rag_chunks_current";
        private String indexVersion = "v2";
        private String analyzerProfile = "standard_zh_en";
        private boolean templateManaged = false;
        private String synonymPath = "config/es/synonyms.txt";
        private String stopwordPath = "config/es/stopwords.txt";
        private String apiKey;
        private String username;
        private String password;
        private Integer timeoutSeconds = 10;
        private boolean autoCreateIndex = true;
    }

    @Data
    public static class Tenant {
        private boolean devHeaderEnabled = true;
        private boolean allowDemoTenantFallback = false;
        private boolean mybatisTenantInterceptorEnabled = true;
        private String objectEnvironment = "prod";
        private String demoUserId = "demo-user";
        private List<String> demoRoles = new ArrayList<>(List.of("TENANT_ADMIN", "KB_OWNER"));
        private List<String> mybatisIgnoreTables = new ArrayList<>(List.of(
                "sys_tenant",
                "sys_platform_admin",
                "model_catalog",
                "rag_model_pricing",
                "tenant_data_deletion_task",
                "tenant_data_deletion_stage"
        ));
        @Min(1)
        private long defaultMaxDocuments = 10000;
        @Min(1)
        private long defaultMaxStorageBytes = 107374182400L;
        @Min(1)
        private long defaultMaxFileBytes = 104857600L;
        @Min(1)
        private long defaultDailyOcrLimit = 10000;
        @Min(1)
        private long defaultDailyEmbeddingTokens = 10000000;
        @Min(1)
        private long defaultMaxConcurrentIngestionTasks = 5;
        @Min(1)
        private long defaultDailyQueryLimit = 100000;
        @Min(1)
        private long defaultMonthlyBudgetCents = 100000;
    }

    @Data
    public static class Security {
        private String mode = "dev";
        private List<String> publicPathPrefixes = new ArrayList<>(List.of(
                "/error",
                "/swagger-ui",
                "/v3/api-docs",
                "/actuator",
                "/api/auth/login",
                "/assets",
                "/favicon.ico"
        ));
        private List<String> adminRoles = new ArrayList<>(List.of("SUPER_ADMIN"));
        /** Whether API key encryption is enabled (default: true). */
        private boolean apiKeyEncryptionEnabled = true;
        /** AES encryption key for API key storage (must be at least 16 characters). */
        private String apiKeyEncryptionKey;
        private final Jwt jwt = new Jwt();
        private final Gateway gateway = new Gateway();

        @Data
        public static class Jwt {
            private String issuer;
            private String jwksUri;
            private String audience;
            private String hmacSecret;
            private String tenantIdClaim = "tenant_id";
            private String tenantExternalIdClaim = "tenant_external_id";
            private String userIdClaim = "sub";
            private String userNameClaim = "name";
            private String rolesClaim = "roles";
            private String knowledgeBaseIdsClaim = "knowledge_base_ids";
            private String permissionTagsClaim = "permission_tags";
            private String requestIdClaim = "jti";
        }

        @Data
        public static class Gateway {
            private String userIdHeader = "X-Gateway-User-Id";
            private String userNameHeader = "X-Gateway-User-Name";
            private String tenantIdHeader = "X-Gateway-Tenant-Id";
            private String tenantExternalIdHeader = "X-Gateway-Tenant-External-Id";
            private String rolesHeader = "X-Gateway-Roles";
            private String knowledgeBaseIdsHeader = "X-Gateway-Knowledge-Base-Ids";
            private String permissionTagsHeader = "X-Gateway-Permission-Tags";
            private String requestIdHeader = "X-Request-Id";
            private String timestampHeader = "X-Gateway-Timestamp";
            private String signatureHeader = "X-Gateway-Signature";
            private String sharedSecret;
            private long maxClockSkewSeconds = 300;
        }
    }

    @Data
    public static class IngestionEvents {
        private String bus = "memory";
        @Min(100)
        private long redisStreamMaxLen = 5000;
        @Min(1)
        private long streamTtlSeconds = 86400;
        @Min(100)
        private long pollIntervalMillis = 1000;
        @Min(1000)
        private long emitterTimeoutMillis = 1800000;
    }

    @Data
    public static class TenantDeletion {
        private boolean enabled = true;
        private boolean executeEnabled = false;
        private boolean mysqlDeleteEnabled = false;
        private boolean objectStorageDeleteEnabled = true;
        private boolean milvusDeleteEnabled = true;
        private boolean elasticsearchDeleteEnabled = true;
        private boolean redisDeleteEnabled = true;
        @Min(1)
        private long lockTtlSeconds = 1800;
        private List<String> mysqlTenantScopedTables = new ArrayList<>(List.of(
                "rag_document_chunk",
                "rag_document_version",
                "rag_document",
                "rag_ingestion_task",
                "rag_image_asset",
                "rag_query_log",
                "rag_tenant_usage_daily",
                "rag_tenant_quota",
                "rag_tenant_model_config",
                "rag_knowledge_base_member",
                "rag_workspace_member",
                "rag_workspace",
                "rag_knowledge_base"
        ));
    }

    @Data
    public static class KeywordReindex {
        private boolean enabled = true;
        @Min(1)
        private int batchSize = 500;
        @Min(1)
        private int validationSampleSize = 20;
        @Min(1)
        private int retainCompletedJobs = 100;
    }

    @Data
    public static class Metrics {
        private boolean materializedEnabled = true;
        private boolean scheduledAggregationEnabled = false;
        @Min(1)
        private int lookbackHours = 48;
        @Min(1)
        private int lateArrivalWindowHours = 2;
        @Min(1000)
        private long fixedDelayMillis = 900000;
        private boolean preferMaterialized = true;
    }

    @Data
    public static class ObjectStorage {
        /** Storage backend type: "local" or "minio". */
        private String type = "local";
        /** MinIO endpoint URL (e.g., http://192.168.220.200:9000). */
        private String endpoint;
        /** MinIO access key. */
        private String accessKey;
        /** MinIO secret key. */
        private String secretKey;
        /** MinIO bucket name. */
        private String bucket = "rag-objects";
    }
}

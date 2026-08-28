package cc.ivera.ragdemo.service.ingest;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ImageEmbeddingRequest;
import cc.ivera.ragdemo.model.knowledge.VisionEmbeddingResult;
import cc.ivera.ragdemo.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ConfigurableVisionEmbeddingClient implements VisionEmbeddingClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Override
    public VisionEmbeddingResult embed(ImageEmbeddingRequest request) {
        RagProperties.MultimodalIngest config = properties.getMultimodalIngest();
        String provider = provider(config);
        String model = model(config);
        int expectedDimension = request != null && request.expectedDimension() != null
                ? request.expectedDimension()
                : config.getVisionEmbeddingDimension();
        if (!config.isEnabled()
                || !config.isVisionEmbeddingEnabled()
                || request == null
                || !StringUtils.hasText(model)
                || !StringUtils.hasText(config.getVisionEmbeddingBaseUrl())) {
            return VisionEmbeddingResult.skipped(provider, model);
        }
        long started = System.nanoTime();
        boolean success = false;
        try {
            String payload = imagePayload(request);
            if (!StringUtils.hasText(payload)) {
                return VisionEmbeddingResult.failed(provider, model, expectedDimension, latencyMs(started), "image input is missing");
            }
            String raw = client(config).post()
                    .uri("/embeddings")
                    .body(requestBody(payload, model))
                    .retrieve()
                    .body(String.class);
            VisionEmbeddingResult result = parseResponse(raw, provider, model, expectedDimension, latencyMs(started));
            success = result.success();
            return result;
        } catch (Exception ex) {
            return VisionEmbeddingResult.failed(provider, model, expectedDimension, latencyMs(started), ex.getMessage());
        } finally {
            metricsService.recordEmbedding(latencyMs(started), success, "vision");
        }
    }

    Map<String, Object> requestBody(String imagePayload, String model) {
        return Map.of(
                "model", model,
                "input", imagePayload
        );
    }

    Map<String, Object> requestBody(ImageEmbeddingRequest request, String model) throws Exception {
        return requestBody(imagePayload(request), model);
    }

    VisionEmbeddingResult parseResponse(String raw,
                                        String provider,
                                        String model,
                                        int expectedDimension,
                                        long latencyMs) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        JsonNode embeddingNode = root.path("data").path(0).path("embedding");
        if (!embeddingNode.isArray()) {
            embeddingNode = root.path("embedding");
        }
        if (!embeddingNode.isArray()) {
            return VisionEmbeddingResult.failed(provider, model, expectedDimension, latencyMs, "embedding array is missing");
        }
        List<Float> vector = new ArrayList<>();
        for (JsonNode node : embeddingNode) {
            if (!node.isNumber()) {
                return VisionEmbeddingResult.failed(provider, model, expectedDimension, latencyMs, "embedding contains non-number value");
            }
            vector.add((float) node.asDouble());
        }
        if (expectedDimension > 0 && vector.size() != expectedDimension) {
            return VisionEmbeddingResult.failed(
                    provider,
                    model,
                    expectedDimension,
                    latencyMs,
                    "embedding dimension mismatch: expected " + expectedDimension + ", actual " + vector.size()
            );
        }
        Integer usage = root.path("usage").path("prompt_tokens").isNumber()
                ? root.path("usage").path("prompt_tokens").asInt()
                : null;
        return new VisionEmbeddingResult("SUCCESS", vector, null, provider, model, vector.size(), usage, latencyMs, null);
    }

    private RestClient client(RagProperties.MultimodalIngest config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(config.getVisionEmbeddingBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory);
        if (StringUtils.hasText(config.getVisionEmbeddingApiKey())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getVisionEmbeddingApiKey());
        }
        return builder.build();
    }

    private String imagePayload(ImageEmbeddingRequest request) throws Exception {
        if (StringUtils.hasText(request.imageBase64())) {
            return request.imageBase64();
        }
        if (request.assetPath() != null && Files.exists(request.assetPath())) {
            return toDataUrl(request.assetPath(), request.mimeType());
        }
        return request.imageUrl();
    }

    private String toDataUrl(Path path, String mimeType) throws Exception {
        String mime = StringUtils.hasText(mimeType) ? mimeType : inferMime(path);
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    private String inferMime(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ? "image/jpeg" : "image/png";
    }

    private String provider(RagProperties.MultimodalIngest config) {
        return StringUtils.hasText(config.getVisionEmbeddingProvider())
                ? config.getVisionEmbeddingProvider().trim().toLowerCase()
                : "unknown";
    }

    private String model(RagProperties.MultimodalIngest config) {
        return StringUtils.hasText(config.getVisionEmbeddingModel())
                ? config.getVisionEmbeddingModel().trim()
                : null;
    }

    private long latencyMs(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }
}

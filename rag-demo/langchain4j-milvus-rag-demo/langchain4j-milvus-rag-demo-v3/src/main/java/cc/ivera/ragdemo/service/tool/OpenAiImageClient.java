package cc.ivera.ragdemo.service.tool;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.service.tenant.ModelConfigService;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.util.ApiKeyEncryptor;
import cc.ivera.ragdemo.util.LogMasker;
import cc.ivera.ragdemo.util.PollingExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OpenAiImageClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final ModelConfigService modelConfigService;
    private final ApiKeyEncryptor apiKeyEncryptor;

    public boolean isEnabled() {
        ImageRequestConfig config = resolveRequestConfig(null);
        return isConfigured(config);
    }

    public ImageResult generate(String prompt, String sizeOverride) {
        ImageRequestConfig imageConfig = resolveRequestConfig(sizeOverride);
        if (!isConfigured(imageConfig)) {
            return new ImageResult("文生图工具未启用，或未配置 image.base-url/image.api-key。", null, List.of());
        }
        if (!StringUtils.hasText(prompt)) {
            return new ImageResult("文生图失败：prompt 不能为空。", null, List.of());
        }

        String apiBaseUrl = imageConfig.apiBaseUrl();
        String endpoint = apiBaseUrl + "/services/aigc/text2image/image-synthesis";
        String apiKey = imageConfig.apiKey();
        String size = imageConfig.size();
        int timeoutSeconds = imageConfig.timeoutSeconds();
        int pollIntervalMillis = imageConfig.pollIntervalMillis();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            String normalizedSize = size == null ? null : size.replace("x", "*").replace("X", "*");
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", imageConfig.modelName());
            body.set("input", objectMapper.createObjectNode().put("prompt", prompt));
            ObjectNode parameters = objectMapper.createObjectNode().put("n", 1);
            if (StringUtils.hasText(normalizedSize)) {
                parameters.put("size", normalizedSize);
            }
            body.set("parameters", parameters);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-DashScope-Async", "enable")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("image generation failed, status=" + response.statusCode() + ", body=" + LogMasker.truncateAndMask(response.body()));
            }

            JsonNode submitRoot = objectMapper.readTree(response.body());
            JsonNode root = waitTaskResult(client, apiBaseUrl, apiKey, submitRoot, timeoutSeconds, pollIntervalMillis);
            String imageUrl = resolveImageUrl(root);
            String revisedPrompt = resolveRevisedPrompt(root);

            if (!StringUtils.hasText(imageUrl)) {
                String message = "已调用文生图接口，但未在响应中解析到图片 URL。";
                return new ImageResult(message, null, List.of(
                        SourceItem.builder()
                                .type(SourceType.IMAGE)
                                .title("Image Generation API")
                                .url(endpoint)
                                .content("响应未包含可直接访问的图片 URL")
                                .build()
                ));
            }

            String summary = "已生成图片：\n" + imageUrl;
            if (StringUtils.hasText(revisedPrompt)) {
                summary += "\n优化后提示词：" + revisedPrompt;
            }

            return new ImageResult(summary, imageUrl, List.of(
                    SourceItem.builder()
                            .type(SourceType.IMAGE)
                            .title("Image Generation API")
                            .url(imageUrl)
                            .content("prompt=" + prompt + (StringUtils.hasText(revisedPrompt) ? ("; revisedPrompt=" + revisedPrompt) : ""))
                            .build()
            ));
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("文生图失败: " + e.getMessage(), e);
        }
    }

    ImageRequestConfig resolveRequestConfig(String sizeOverride) {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        RagTenantModelConfig config = modelConfigService.getActiveImageConfig(tenantId);
        String modelName = resolveText(config == null ? null : config.getModelName(), properties.getImage().getModel());
        String baseUrl = resolveText(config == null ? null : config.getBaseUrl(), resolvePropertiesBaseUrl());
        String apiKey = resolveText(decryptApiKey(config == null ? null : config.getApiKeySecretRef()), resolvePropertiesApiKey());
        String size = resolveText(sizeOverride, resolveText(config == null ? null : config.getImageSize(), properties.getImage().getSize()));
        String quality = resolveText(config == null ? null : config.getImageQuality(), properties.getImage().getQuality());
        int timeoutSeconds = resolvePositive(config == null ? null : config.getTimeoutSeconds(), properties.getImage().getTimeoutSeconds(), 60);
        int pollIntervalMillis = resolvePositive(
                config == null ? null : config.getPollIntervalMillis(),
                properties.getImage().getPollIntervalMillis(),
                2000);
        return new ImageRequestConfig(
                modelName,
                baseUrl,
                resolveImageApiBaseUrl(baseUrl),
                apiKey,
                size,
                quality,
                timeoutSeconds,
                pollIntervalMillis
        );
    }

    private boolean isConfigured(ImageRequestConfig config) {
        return properties.getImage().isEnabled()
                && StringUtils.hasText(config.baseUrl())
                && StringUtils.hasText(config.apiKey())
                && StringUtils.hasText(config.modelName());
    }

    private String resolvePropertiesBaseUrl() {
        if (StringUtils.hasText(properties.getImage().getBaseUrl())) {
            return properties.getImage().getBaseUrl();
        }
        return properties.getLlm().getBaseUrl();
    }

    private String resolveImageApiBaseUrl(String rawBaseUrl) {
        String baseUrl = normalizeBaseUrl(rawBaseUrl);
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        if (baseUrl.contains("/compatible-mode/")) {
            return baseUrl.replace("/compatible-mode/v1", "/api/v1");
        }
        return baseUrl;
    }

    private String resolvePropertiesApiKey() {
        if (StringUtils.hasText(properties.getImage().getApiKey())) {
            return properties.getImage().getApiKey();
        }
        return properties.getLlm().getApiKey();
    }

    private String normalizeBaseUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String url = raw.trim();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String decryptApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        return apiKeyEncryptor.decrypt(apiKey);
    }

    private String resolveText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private int resolvePositive(Integer primary, Integer fallback, int defaultValue) {
        if (primary != null && primary > 0) {
            return primary;
        }
        if (fallback != null && fallback > 0) {
            return fallback;
        }
        return defaultValue;
    }

    private String resolveImageUrl(JsonNode root) {
        if (root.path("output").path("choices").isArray() && root.path("output").path("choices").size() > 0) {
            JsonNode firstChoice = root.path("output").path("choices").get(0);
            JsonNode content = firstChoice.path("message").path("content");
            if (content.isArray() && content.size() > 0) {
                for (JsonNode item : content) {
                    String image = item.path("image").asText(null);
                    if (StringUtils.hasText(image)) {
                        return image;
                    }
                }
            }
        }
        if (root.path("data").isArray() && root.path("data").size() > 0) {
            JsonNode first = root.path("data").get(0);
            String url = first.path("url").asText(null);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        if (root.path("output").path("results").isArray() && root.path("output").path("results").size() > 0) {
            String url = root.path("output").path("results").get(0).path("url").asText(null);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    private JsonNode waitTaskResult(HttpClient client,
                                    String apiBaseUrl,
                                    String apiKey,
                                    JsonNode submitRoot,
                                    int timeoutSeconds,
                                    int pollIntervalMillis) throws IOException {
        String taskId = submitRoot.path("output").path("task_id").asText(null);
        if (!StringUtils.hasText(taskId)) {
            return submitRoot;
        }

        // 使用非阻塞轮询替代 Thread.sleep 阻塞轮询
        return PollingExecutor.pollSyncWithError(() -> {
            HttpRequest queryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/tasks/" + taskId))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> queryResponse = client.send(queryRequest, HttpResponse.BodyHandlers.ofString());
            if (queryResponse.statusCode() < 200 || queryResponse.statusCode() >= 300) {
                throw new IllegalStateException("query image task failed, status=" + queryResponse.statusCode() + ", body=" + LogMasker.truncateAndMask(queryResponse.body()));
            }

            JsonNode taskRoot = objectMapper.readTree(queryResponse.body());
            String taskStatus = taskRoot.path("output").path("task_status").asText("");
            if ("SUCCEEDED".equalsIgnoreCase(taskStatus)) {
                return PollingExecutor.PollingResult.success(taskRoot);
            }
            if ("FAILED".equalsIgnoreCase(taskStatus) || "CANCELED".equalsIgnoreCase(taskStatus)) {
                throw new IllegalStateException("image task failed, taskId=" + taskId + ", body=" + LogMasker.truncateAndMask(queryResponse.body()));
            }

            // 返回 null 表示继续轮询
            return null;
        }, Math.max(500, pollIntervalMillis), timeoutSeconds, "image task failed, taskId=" + taskId);
    }

    private String resolveRevisedPrompt(JsonNode root) {
        if (root.path("data").isArray() && root.path("data").size() > 0) {
            String revisedPrompt = root.path("data").get(0).path("revised_prompt").asText(null);
            if (StringUtils.hasText(revisedPrompt)) {
                return revisedPrompt;
            }
        }
        return null;
    }

    public record ImageResult(String summary, String imageUrl, List<SourceItem> sources) {
    }

    record ImageRequestConfig(String modelName,
                              String baseUrl,
                              String apiBaseUrl,
                              String apiKey,
                              String size,
                              String quality,
                              int timeoutSeconds,
                              int pollIntervalMillis) {
    }
}

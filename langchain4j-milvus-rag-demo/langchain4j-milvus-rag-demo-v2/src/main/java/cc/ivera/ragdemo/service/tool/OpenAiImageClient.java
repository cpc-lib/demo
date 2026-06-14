package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OpenAiImageClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return properties.getImage().isEnabled()
                && StringUtils.hasText(resolveBaseUrl())
                && StringUtils.hasText(resolveApiKey());
    }

    public ImageResult generate(String prompt, String sizeOverride) {
        if (!isEnabled()) {
            return new ImageResult("文生图工具未启用，或未配置 image.base-url/image.api-key。", null, List.of());
        }
        if (!StringUtils.hasText(prompt)) {
            return new ImageResult("文生图失败：prompt 不能为空。", null, List.of());
        }

        String apiBaseUrl = resolveImageApiBaseUrl();
        String endpoint = apiBaseUrl + "/services/aigc/text2image/image-synthesis";
        String apiKey = resolveApiKey();
        String size = StringUtils.hasText(sizeOverride) ? sizeOverride.trim() : properties.getImage().getSize();
        int timeoutSeconds = properties.getImage().getTimeoutSeconds() == null ? 60 : properties.getImage().getTimeoutSeconds();
        int pollIntervalMillis = properties.getImage().getPollIntervalMillis() == null ? 2000 : properties.getImage().getPollIntervalMillis();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            String normalizedSize = size == null ? null : size.replace("x", "*").replace("X", "*");
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getImage().getModel());
            body.set("input", objectMapper.createObjectNode().put("prompt", prompt));
            body.set("parameters", objectMapper.createObjectNode()
                    .put("size", normalizedSize)
                    .put("n", 1));

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
                throw new IllegalStateException("image generation failed, status=" + response.statusCode() + ", body=" + response.body());
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("文生图失败: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("文生图失败: " + e.getMessage(), e);
        }
    }

    private String resolveBaseUrl() {
        if (StringUtils.hasText(properties.getImage().getBaseUrl())) {
            return properties.getImage().getBaseUrl();
        }
        return properties.getLlm().getBaseUrl();
    }

    private String resolveImageApiBaseUrl() {
        String baseUrl = normalizeBaseUrl(resolveBaseUrl());
        if (baseUrl.contains("/compatible-mode/")) {
            return baseUrl.replace("/compatible-mode/v1", "/api/v1");
        }
        return baseUrl;
    }

    private String resolveApiKey() {
        if (StringUtils.hasText(properties.getImage().getApiKey())) {
            return properties.getImage().getApiKey();
        }
        return properties.getLlm().getApiKey();
    }

    private String normalizeBaseUrl(String raw) {
        String url = raw.trim();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
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
                                    int pollIntervalMillis) throws IOException, InterruptedException {
        String taskId = submitRoot.path("output").path("task_id").asText(null);
        if (!StringUtils.hasText(taskId)) {
            return submitRoot;
        }

        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() <= deadline) {
            HttpRequest queryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/tasks/" + taskId))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> queryResponse = client.send(queryRequest, HttpResponse.BodyHandlers.ofString());
            if (queryResponse.statusCode() < 200 || queryResponse.statusCode() >= 300) {
                throw new IllegalStateException("query image task failed, status=" + queryResponse.statusCode() + ", body=" + queryResponse.body());
            }

            JsonNode taskRoot = objectMapper.readTree(queryResponse.body());
            String taskStatus = taskRoot.path("output").path("task_status").asText("");
            if ("SUCCEEDED".equalsIgnoreCase(taskStatus)) {
                return taskRoot;
            }
            if ("FAILED".equalsIgnoreCase(taskStatus) || "CANCELED".equalsIgnoreCase(taskStatus)) {
                throw new IllegalStateException("image task failed, taskId=" + taskId + ", body=" + queryResponse.body());
            }

            Thread.sleep(Math.max(500, pollIntervalMillis));
        }

        throw new IllegalStateException("image task timeout, task still running: " + taskId);
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
}

package cc.ivera.ragdemo.service.ingest;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class VisionStructuredAnalysisClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public String analyze(ExtractedImageKnowledge image) {
        RagProperties.MultimodalIngest config = properties.getMultimodalIngest();
        if (!config.isEnabled()
                || !config.isVisionAnalysisEnabled()
                || !StringUtils.hasText(resolveApiKey(config))
                || !StringUtils.hasText(resolveModel(config))
                || !hasImageInput(image)) {
            return "{}";
        }

        try {
            String imageDataUrl = imagePayload(image);
            String prompt = """
                    You are extracting enterprise knowledge from a document image.
                    Return strict JSON only. Do not wrap it in Markdown.

                    Required top-level fields:
                    - contentType: one of image, chart, flowchart, architecture, table, diagram
                    - title
                    - caption
                    - imageNumber
                    - sectionTitle
                    - ocrText
                    - entities
                    - relationships
                    - steps
                    - chartType
                    - xAxis
                    - yAxis
                    - series
                    - insights
                    - summary

                    For charts, extract axes, series names and data points when visible.
                    For flowcharts, extract nodes, directed edges, branch conditions and step order.
                    For architecture diagrams, extract components, layers, dependencies, protocols and data flows.

                    Document context:
                    sectionTitle=%s
                    imageCaption=%s
                    imageNumber=%s
                    existingOcrText=%s
                    previousText=%s
                    nextText=%s
                    """.formatted(
                    nullToEmpty(image.sectionTitle()),
                    nullToEmpty(image.imageCaption()),
                    nullToEmpty(image.imageNumber()),
                    nullToEmpty(image.ocrText()),
                    nullToEmpty(image.previousText()),
                    nullToEmpty(image.nextText())
            );

            Map<String, Object> request = Map.of(
                    "model", resolveModel(config),
                    "temperature", 0,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", prompt),
                                    Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))
                            )
                    ))
            );

            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
            requestFactory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));

            RestClient client = RestClient.builder()
                    .baseUrl(resolveBaseUrl(config))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resolveApiKey(config))
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .requestFactory(requestFactory)
                    .build();

            String body = client.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body);
            String content = root.path("choices").path(0).path("message").path("content").asText("{}");
            return normalizeJson(content);
        } catch (Exception e) {
            return "{\"analysisStatus\":\"failed\",\"reason\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String toDataUrl(Path path) throws Exception {
        String fileName = path.getFileName().toString().toLowerCase();
        String mime = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ? "image/jpeg" : "image/png";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    private boolean hasImageInput(ExtractedImageKnowledge image) {
        return image != null
                && (image.assetPath() != null || StringUtils.hasText(image.imageUrl()));
    }

    private String imagePayload(ExtractedImageKnowledge image) throws Exception {
        if (image.assetPath() != null) {
            return toDataUrl(image.assetPath());
        }
        return image.imageUrl();
    }

    private String normalizeJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        return trimmed;
    }

    private String resolveBaseUrl(RagProperties.MultimodalIngest config) {
        if (StringUtils.hasText(config.getVisionBaseUrl())) {
            return config.getVisionBaseUrl();
        }
        return properties.getLlm().getBaseUrl();
    }

    private String resolveApiKey(RagProperties.MultimodalIngest config) {
        if (StringUtils.hasText(config.getVisionApiKey())) {
            return config.getVisionApiKey();
        }
        return properties.getLlm().getApiKey();
    }

    private String resolveModel(RagProperties.MultimodalIngest config) {
        if (StringUtils.hasText(config.getVisionModel())) {
            return config.getVisionModel();
        }
        return properties.getLlm().getModel();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package cc.ivera.ragdemo.service.ingest;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import cc.ivera.ragdemo.model.knowledge.OcrExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
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

@Primary
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ExternalOcrTextExtractor implements OcrTextExtractor {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String extract(ExtractedImageKnowledge image) {
        return extractResult(image).text();
    }

    @Override
    public OcrExtractionResult extractResult(ExtractedImageKnowledge image) {
        RagProperties.MultimodalIngest config = properties.getMultimodalIngest();
        String provider = normalizeProvider(config.getOcrProvider());
        String model = StringUtils.hasText(config.getOcrModel()) ? config.getOcrModel().trim() : null;
        if (!config.isEnabled()
                || !config.isOcrEnabled()
                || "noop".equals(provider)
                || image == null
                || (!StringUtils.hasText(image.imageUrl()) && image.assetPath() == null)) {
            return OcrExtractionResult.skipped(image == null ? null : image.ocrText());
        }
        if (!StringUtils.hasText(config.getOcrBaseUrl())) {
            return OcrExtractionResult.failed(provider, model, "OCR base URL is not configured");
        }
        try {
            long started = System.nanoTime();
            RestClient client = client(config);
            String raw;
            if ("openai".equals(provider) || "dashscope".equals(provider) || "openai-compatible".equals(provider)) {
                raw = client.post()
                        .uri("/chat/completions")
                        .body(openAiCompatibleRequest(image, config))
                        .retrieve()
                        .body(String.class);
            } else {
                raw = client.post()
                        .uri("/ocr")
                        .body(genericRequest(image, config))
                        .retrieve()
                        .body(String.class);
            }
            OcrExtractionResult parsed = parseResponse(raw, provider, model);
            return new OcrExtractionResult(
                    parsed.status(),
                    firstText(parsed.text(), image.ocrText()),
                    parsed.confidence(),
                    parsed.provider(),
                    parsed.model(),
                    parsed.rawJson(),
                    parsed.errorMessage()
            );
        } catch (Exception ex) {
            return OcrExtractionResult.failed(provider, model, ex.getMessage());
        }
    }

    Map<String, Object> genericRequest(ExtractedImageKnowledge image, RagProperties.MultimodalIngest config) throws Exception {
        return Map.of(
                "model", nullToEmpty(config.getOcrModel()),
                "image", imagePayload(image),
                "imageUrl", nullToEmpty(image.imageUrl()),
                "returnBlocks", true
        );
    }

    Map<String, Object> openAiCompatibleRequest(ExtractedImageKnowledge image, RagProperties.MultimodalIngest config) throws Exception {
        String prompt = """
                Extract OCR text from this image.
                Return strict JSON only with fields: text, confidence, blocks.
                blocks items should contain text, confidence and bbox when available.
                """;
        return Map.of(
                "model", nullToEmpty(config.getOcrModel()),
                "temperature", 0,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", prompt),
                                Map.of("type", "image_url", "image_url", Map.of("url", imagePayload(image)))
                        )
                ))
        );
    }

    OcrExtractionResult parseResponse(String raw, String provider, String fallbackModel) {
        if (!StringUtils.hasText(raw)) {
            return OcrExtractionResult.failed(provider, fallbackModel, "empty OCR response");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode payload = root;
            JsonNode choiceContent = root.path("choices").path(0).path("message").path("content");
            if (choiceContent.isTextual() && StringUtils.hasText(choiceContent.asText())) {
                payload = objectMapper.readTree(stripMarkdownFence(choiceContent.asText()));
            }
            String text = firstText(payload.path("text").asText(null), payload.path("ocrText").asText(null));
            double confidence = payload.path("confidence").isNumber() ? payload.path("confidence").asDouble() : 0.0D;
            String model = firstText(payload.path("model").asText(null), fallbackModel);
            return OcrExtractionResult.success(text, clamp(confidence), provider, model, objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            return OcrExtractionResult.failed(provider, fallbackModel, ex.getMessage());
        }
    }

    private RestClient client(RagProperties.MultimodalIngest config) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(config.getOcrBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory);
        if (StringUtils.hasText(config.getOcrApiKey())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getOcrApiKey());
        }
        return builder.build();
    }

    private String imagePayload(ExtractedImageKnowledge image) throws Exception {
        if (image.assetPath() != null && Files.exists(image.assetPath())) {
            return toDataUrl(image.assetPath());
        }
        return image.imageUrl();
    }

    private String toDataUrl(Path path) throws Exception {
        String fileName = path.getFileName().toString().toLowerCase();
        String mime = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ? "image/jpeg" : "image/png";
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
    }

    private String normalizeProvider(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "noop";
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            return trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        return trimmed;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VisualStructuredContentValidator {

    private final ObjectMapper objectMapper;
    private final RagProperties properties;
    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
    private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();

    @Autowired
    public VisualStructuredContentValidator(ObjectMapper objectMapper, RagProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public VisualStructuredContentValidator(ObjectMapper objectMapper) {
        this(objectMapper, new RagProperties());
    }

    public VisualValidationResult validate(String rawJson) {
        return validate(rawJson, properties.getMultimodalIngest().getVisualSchemaPath());
    }

    public VisualValidationResult validate(String rawJson, String schemaPath) {
        if (!StringUtils.hasText(rawJson)) {
            return failed("empty visual analysis result");
        }
        try {
            JsonNode parsed = objectMapper.readTree(rawJson);
            if (!parsed.isObject()) {
                return failed("visual analysis result must be a JSON object");
            }
            JsonNode schema = loadSchema(schemaPath);
            List<String> errors = new ArrayList<>();
            validateNode("$", parsed, schema, errors);

            ObjectNode normalized = ((ObjectNode) parsed).deepCopy();
            boolean schemaValid = errors.isEmpty();
            double confidence = resolveConfidence(normalized, schemaValid, errors.size());
            String status = schemaValid ? "SUCCESS" : "INVALID";
            normalized.put("analysisStatus", schemaValid ? "success" : "invalid");
            normalized.put("schemaValid", schemaValid);
            normalized.put("confidence", confidence);
            ArrayNode errorNodes = objectMapper.createArrayNode();
            errors.forEach(errorNodes::add);
            normalized.set("schemaErrors", errorNodes);

            return new VisualValidationResult(
                    status,
                    schemaValid,
                    confidence,
                    objectMapper.writeValueAsString(normalized),
                    objectMapper.writeValueAsString(errors)
            );
        } catch (Exception e) {
            return failed(e.getMessage());
        }
    }

    private JsonNode loadSchema(String schemaPath) throws Exception {
        String path = StringUtils.hasText(schemaPath) ? schemaPath.trim() : "classpath:schema/visual-knowledge.schema.json";
        JsonNode cached = schemaCache.get(path);
        if (cached != null) {
            return cached;
        }
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Visual schema file not found: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            JsonNode schema = objectMapper.readTree(in);
            schemaCache.put(path, schema);
            return schema;
        }
    }

    private void validateNode(String path, JsonNode value, JsonNode schema, List<String> errors) {
        if (schema == null || schema.isMissingNode() || schema.isNull()) {
            return;
        }
        validateType(path, value, schema, errors);
        validateEnum(path, value, schema, errors);
        validateRange(path, value, schema, errors);

        JsonNode required = schema.path("required");
        if (required.isArray() && value.isObject()) {
            for (JsonNode field : required) {
                String name = field.asText();
                JsonNode child = value.path(name);
                if (child.isMissingNode() || child.isNull() || (child.isTextual() && !StringUtils.hasText(child.asText()))) {
                    errors.add(path + "." + name + " is required");
                }
            }
        }

        JsonNode propertiesNode = schema.path("properties");
        if (propertiesNode.isObject() && value.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = value.path(entry.getKey());
                if (!child.isMissingNode() && !child.isNull()) {
                    validateNode(path + "." + entry.getKey(), child, entry.getValue(), errors);
                }
            }
        }

        JsonNode itemSchema = schema.path("items");
        if (itemSchema.isObject() && value.isArray()) {
            for (int i = 0; i < value.size(); i++) {
                validateNode(path + "[" + i + "]", value.get(i), itemSchema, errors);
            }
        }
    }

    private void validateType(String path, JsonNode value, JsonNode schema, List<String> errors) {
        JsonNode typeNode = schema.path("type");
        if (!typeNode.isTextual() || value == null || value.isMissingNode() || value.isNull()) {
            return;
        }
        String type = typeNode.asText();
        boolean ok = switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "integer" -> value.isIntegralNumber();
            case "boolean" -> value.isBoolean();
            default -> true;
        };
        if (!ok) {
            errors.add(path + " must be " + type);
        }
    }

    private void validateEnum(String path, JsonNode value, JsonNode schema, List<String> errors) {
        JsonNode enumNode = schema.path("enum");
        if (!enumNode.isArray() || value == null || value.isMissingNode() || value.isNull()) {
            return;
        }
        for (JsonNode allowed : enumNode) {
            if (allowed.equals(value) || allowed.asText().equals(value.asText())) {
                return;
            }
        }
        errors.add(path + " must be one of " + enumNode);
    }

    private void validateRange(String path, JsonNode value, JsonNode schema, List<String> errors) {
        if (value == null || !value.isNumber()) {
            return;
        }
        if (schema.has("minimum") && value.asDouble() < schema.path("minimum").asDouble()) {
            errors.add(path + " must be >= " + schema.path("minimum").asDouble());
        }
        if (schema.has("maximum") && value.asDouble() > schema.path("maximum").asDouble()) {
            errors.add(path + " must be <= " + schema.path("maximum").asDouble());
        }
    }

    private VisualValidationResult failed(String reason) {
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("analysisStatus", "failed");
        normalized.put("schemaValid", false);
        normalized.put("confidence", 0.0D);
        normalized.put("errorMessage", reason == null ? "" : reason);
        ArrayNode errors = objectMapper.createArrayNode();
        errors.add(reason == null ? "unknown validation error" : reason);
        normalized.set("schemaErrors", errors);
        try {
            return new VisualValidationResult(
                    "FAILED",
                    false,
                    0.0D,
                    objectMapper.writeValueAsString(normalized),
                    objectMapper.writeValueAsString(List.of(reason == null ? "unknown validation error" : reason))
            );
        } catch (Exception e) {
            return new VisualValidationResult("FAILED", false, 0.0D,
                    "{\"analysisStatus\":\"failed\",\"schemaValid\":false,\"confidence\":0.0}",
                    "[\"validation failure\"]");
        }
    }

    private double resolveConfidence(ObjectNode normalized, boolean schemaValid, int errorCount) {
        JsonNode confidenceNode = normalized.path("confidence");
        if (confidenceNode.isNumber()) {
            return clamp(confidenceNode.asDouble());
        }
        if (confidenceNode.isTextual()) {
            try {
                return clamp(Double.parseDouble(confidenceNode.asText()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (schemaValid) {
            return 0.8D;
        }
        return Math.max(0.2D, 0.8D - (0.15D * errorCount));
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

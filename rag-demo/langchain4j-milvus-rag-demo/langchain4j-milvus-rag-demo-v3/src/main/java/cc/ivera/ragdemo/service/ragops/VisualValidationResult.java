package cc.ivera.ragdemo.service.ragops;

public record VisualValidationResult(
        String status,
        boolean schemaValid,
        double confidence,
        String normalizedJson,
        String schemaErrors
) {

    public VisualValidationResult(String status,
                                  boolean schemaValid,
                                  double confidence,
                                  String normalizedJson) {
        this(status, schemaValid, confidence, normalizedJson, null);
    }
}

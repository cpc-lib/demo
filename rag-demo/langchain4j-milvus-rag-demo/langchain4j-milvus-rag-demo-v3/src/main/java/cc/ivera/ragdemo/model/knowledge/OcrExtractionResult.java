package cc.ivera.ragdemo.model.knowledge;

public record OcrExtractionResult(
        String status,
        String text,
        Double confidence,
        String provider,
        String model,
        String rawJson,
        String errorMessage
) {

    public static OcrExtractionResult skipped(String text) {
        return new OcrExtractionResult("SKIPPED", text, null, "noop", null, null, null);
    }

    public static OcrExtractionResult success(String text,
                                              Double confidence,
                                              String provider,
                                              String model,
                                              String rawJson) {
        return new OcrExtractionResult("SUCCESS", text, confidence, provider, model, rawJson, null);
    }

    public static OcrExtractionResult failed(String provider,
                                             String model,
                                             String errorMessage) {
        return new OcrExtractionResult("FAILED", null, 0.0D, provider, model, null, errorMessage);
    }
}

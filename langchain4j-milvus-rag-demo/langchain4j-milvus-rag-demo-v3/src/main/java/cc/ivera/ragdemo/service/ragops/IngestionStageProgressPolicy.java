package cc.ivera.ragdemo.service.ragops;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IngestionStageProgressPolicy {

    public static final String OBJECT_READ = "OBJECT_READ";
    public static final String PARSE_DOCUMENT = "PARSE_DOCUMENT";
    public static final String EXTRACT_ASSETS = "EXTRACT_ASSETS";
    public static final String SPLIT_CHUNKS = "SPLIT_CHUNKS";
    public static final String OCR_IMAGES = "OCR_IMAGES";
    public static final String VISION_ANALYSIS = "VISION_ANALYSIS";
    public static final String EMBED_TEXT = "EMBED_TEXT";
    public static final String EMBED_IMAGE = "EMBED_IMAGE";
    public static final String WRITE_VECTOR = "WRITE_VECTOR";
    public static final String PERSIST_METADATA = "PERSIST_METADATA";
    public static final String SYNC_KEYWORD_INDEX = "SYNC_KEYWORD_INDEX";

    private static final List<StagePlan> DEFAULT_STAGES = List.of(
            new StagePlan(OBJECT_READ, "Read object file", 10, 5),
            new StagePlan(PARSE_DOCUMENT, "Parse document", 20, 15),
            new StagePlan(EXTRACT_ASSETS, "Extract assets", 30, 10),
            new StagePlan(SPLIT_CHUNKS, "Split chunks", 40, 10),
            new StagePlan(OCR_IMAGES, "OCR images", 50, 10),
            new StagePlan(VISION_ANALYSIS, "Vision analysis", 60, 10),
            new StagePlan(EMBED_TEXT, "Embed text", 70, 15),
            new StagePlan(EMBED_IMAGE, "Embed images", 80, 10),
            new StagePlan(WRITE_VECTOR, "Write vectors", 90, 10),
            new StagePlan(PERSIST_METADATA, "Persist metadata", 100, 3),
            new StagePlan(SYNC_KEYWORD_INDEX, "Sync keyword index", 110, 2)
    );

    private IngestionStageProgressPolicy() {
    }

    public static List<StagePlan> defaultStages() {
        return DEFAULT_STAGES;
    }

    public static Map<String, StagePlan> defaultStageMap() {
        Map<String, StagePlan> plans = new LinkedHashMap<>();
        for (StagePlan plan : DEFAULT_STAGES) {
            plans.put(plan.stageCode(), plan);
        }
        return plans;
    }

    public static int weightedProgress(List<StagePlan> stages, Map<String, Integer> stageProgress) {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }
        int totalWeight = 0;
        int weighted = 0;
        for (StagePlan stage : stages) {
            int weight = Math.max(0, stage.weight());
            totalWeight += weight;
            weighted += weight * clampProgress(stageProgress == null ? null : stageProgress.get(stage.stageCode()));
        }
        if (totalWeight == 0) {
            return 0;
        }
        return Math.min(100, Math.max(0, Math.round(weighted / (float) totalWeight)));
    }

    public static int unitProgress(int successCount, int failedCount, int totalCount) {
        if (totalCount <= 0) {
            return 0;
        }
        int done = Math.max(0, successCount) + Math.max(0, failedCount);
        return clampProgress(Math.round(done * 100f / totalCount));
    }

    public static int clampProgress(Integer progress) {
        if (progress == null) {
            return 0;
        }
        return Math.min(100, Math.max(0, progress));
    }

    public record StagePlan(String stageCode, String stageName, int stageOrder, int weight) {
    }
}

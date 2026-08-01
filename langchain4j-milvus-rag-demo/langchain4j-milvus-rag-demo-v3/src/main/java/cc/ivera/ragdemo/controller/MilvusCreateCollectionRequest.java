package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MilvusCreateCollectionRequest(
        String databaseName,
        @NotBlank String collectionName,
        String description,
        @Min(1) int dimension,
        String primaryFieldName,
        String idType,
        @Min(1) Integer maxLength,
        String vectorFieldName,
        String metricType,
        Boolean autoId,
        Boolean enableDynamicField,
        @Min(1) Integer numShards
) {

    public String databaseNameOrDefault() {
        return (databaseName == null || databaseName.isBlank()) ? "default" : databaseName;
    }

    public String primaryFieldNameOrDefault() {
        return (primaryFieldName == null || primaryFieldName.isBlank()) ? "id" : primaryFieldName;
    }

    public String vectorFieldNameOrDefault() {
        return (vectorFieldName == null || vectorFieldName.isBlank()) ? "vector" : vectorFieldName;
    }

    public String metricTypeOrDefault() {
        return (metricType == null || metricType.isBlank()) ? "COSINE" : metricType;
    }

    public boolean autoIdOrDefault() {
        return autoId != null && autoId;
    }

    public boolean enableDynamicFieldOrDefault() {
        return enableDynamicField == null || enableDynamicField;
    }

    public int numShardsOrDefault() {
        return numShards == null ? 1 : numShards;
    }

    public int maxLengthOrDefault() {
        return maxLength == null ? 512 : maxLength;
    }
}
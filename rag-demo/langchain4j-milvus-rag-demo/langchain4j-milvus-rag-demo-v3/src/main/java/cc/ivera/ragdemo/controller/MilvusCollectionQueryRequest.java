package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MilvusCollectionQueryRequest(
        String databaseName,
        @NotBlank String collectionName,
        String filter,
        List<String> outputFields,
        List<String> partitionNames,
        @Min(0) long offset,
        @Min(1) long limit,
        boolean loadBeforeQuery
) {
    public String safeDatabaseName() {
        return (databaseName == null || databaseName.isBlank()) ? "default" : databaseName.trim();
    }

    public String safeFilter() {
        return filter == null ? "" : filter.trim();
    }

    public long safeLimit() {
        return limit <= 0 ? 10 : limit;
    }
}

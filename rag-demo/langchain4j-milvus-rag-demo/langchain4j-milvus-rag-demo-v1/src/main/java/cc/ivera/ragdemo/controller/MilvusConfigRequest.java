package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.service.vector.MilvusStoreConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MilvusConfigRequest(
        @NotBlank String alias,
        @NotBlank String host,
        @Min(1) int port,
        @NotBlank String collection,
        @Min(1) int topK,
        double minScore
) {
    public MilvusStoreConfig toStoreConfig() {
        return MilvusStoreConfig.builder()
                .alias(alias)
                .host(host)
                .port(port)
                .collection(collection)
                .topK(topK)
                .minScore(minScore)
                .build();
    }
}

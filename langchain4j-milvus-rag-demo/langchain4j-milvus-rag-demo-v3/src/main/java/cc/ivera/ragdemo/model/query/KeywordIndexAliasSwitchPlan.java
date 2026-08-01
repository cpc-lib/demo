package cc.ivera.ragdemo.model.query;

public record KeywordIndexAliasSwitchPlan(
        String alias,
        String fromIndex,
        String toIndex,
        String engineCompatible,
        String requestPath,
        String requestBody
) {
}

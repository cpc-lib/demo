package cc.ivera.ragdemo.model;

import lombok.Builder;

@Builder
public record ToolTrace(String toolName, String summary) {
}

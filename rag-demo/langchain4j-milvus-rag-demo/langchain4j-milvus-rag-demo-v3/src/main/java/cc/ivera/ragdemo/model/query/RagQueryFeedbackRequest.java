package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.NotBlank;

public record RagQueryFeedbackRequest(
        @NotBlank(message = "rating must not be blank")
        String rating,
        String createdBy,
        String comment,
        String correctedAnswer
) {
}

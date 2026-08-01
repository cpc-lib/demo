package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModelConfigApiKeyUpdateRequest(
        @NotBlank(message = "API Key 或密钥引用不能为空")
        @Size(max = 2048, message = "API Key 或密钥引用长度不能超过2048个字符")
        String apiKeySecretRef
) {
}

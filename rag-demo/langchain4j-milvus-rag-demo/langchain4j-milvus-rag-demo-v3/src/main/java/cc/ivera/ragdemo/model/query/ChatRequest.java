package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求 DTO：聊天接口参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 10000, message = "问题长度必须在1-10000个字符之间")
    private String question;

    /**
     * 会话ID（可选）
     */
    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    private String conversationId;
}
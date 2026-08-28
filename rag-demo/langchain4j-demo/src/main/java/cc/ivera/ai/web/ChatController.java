package cc.ivera.ai.web;

import cc.ivera.ai.service.ChatAssistant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatAssistant chatAssistant;

    public ChatController(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }

    @PostMapping
    public String chat(@RequestBody ChatReq req) {
        if (req.sessionId() == null || req.sessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        return chatAssistant.chat(req.sessionId(), req.message());
    }

    public record ChatReq(String sessionId, String message) {
    }
}

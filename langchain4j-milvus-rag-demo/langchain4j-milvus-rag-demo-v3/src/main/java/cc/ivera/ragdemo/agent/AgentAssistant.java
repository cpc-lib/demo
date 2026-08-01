package cc.ivera.ragdemo.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

public interface AgentAssistant {

    Result<String> chat(@MemoryId String conversationId, @UserMessage String userMessage);
}

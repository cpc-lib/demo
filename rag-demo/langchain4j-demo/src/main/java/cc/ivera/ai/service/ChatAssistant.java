package cc.ivera.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {

    @SystemMessage("""
            你是一个专业的 Java/架构/AI 工程助手。
            回答要结构清晰，优先给可执行方案与代码。
            当检索到知识库内容时，优先基于知识库回答；没有则基于常识回答。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}

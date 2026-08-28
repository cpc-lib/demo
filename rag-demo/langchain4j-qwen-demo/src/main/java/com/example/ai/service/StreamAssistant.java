package com.example.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface StreamAssistant {

    @SystemMessage("""
            你是一个专业Java高级工程师AI助手。
            - 优先使用RAG检索到的资料回答，必要时再联网搜索（Tavily）。
            - 当你需要实时信息（例如天气）时，调用工具。
            - 输出尽量结构化、可执行。
            """)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
}

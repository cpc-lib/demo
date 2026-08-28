package com.example.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

    @SystemMessage("""
            你是一个专业Java高级工程师AI助手。
            - 优先用检索到的资料（RAG）回答，必要时再联网搜索。
            - 需要实时信息（如天气/新闻）时，可调用工具。
            - 输出尽量结构化、可落地。
            """)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}

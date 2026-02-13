# langchain4j-qwen-rag-mysql-sse

功能：
- Spring Boot 3 + LangChain4j 1.11.0
- 模型：qwen-plus（DashScope OpenAI兼容模式）
- RAG：resources/rag 下文档切分 + embedding(text-embedding-v4) + InMemoryEmbeddingStore
- Memory：MySQL 持久化 ChatMemoryStore
- Tools：Tavily 联网搜索 + 天气查询（基于Tavily）
- SSE：Agent级事件流（token/tool_start/tool_end/done）
- 文生图：qwen-image-plus（DashScope multimodal-generation）

## 运行
1) 启动 MySQL
```bash
docker-compose up -d
```

2) 设置环境变量
```bash
export DASHSCOPE_API_KEY=sk-xxx
export TAVILY_API_KEY=tvly-xxx
```

3) 启动
```bash
mvn spring-boot:run
```

## API
- POST http://localhost:8080/api/chat
- POST http://localhost:8080/api/chat/stream  (SSE)
- POST http://localhost:8080/api/image

SSE 事件：
- token
- tool_start
- tool_end
- done

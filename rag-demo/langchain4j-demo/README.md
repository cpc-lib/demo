# LangChain4j(1.11.0) + Spring Boot + Qwen(+RAG+MySQL Memory) + Qwen Image

## 1. 环境要求

- JDK 17+
- Maven 3.8+
- Docker（启动 MySQL）

## 2. 启动 MySQL

```bash
docker compose up -d
```

## 3. 配置 DashScope API Key

- Windows PowerShell:

```powershell
setx DASHSCOPE_API_KEY "sk-xxx"
```

- macOS/Linux:

```bash
export DASHSCOPE_API_KEY="sk-xxx"
```

## 4. 启动服务

```bash
mvn -q -DskipTests spring-boot:run
```

## 5. 调用接口

### 5.1 对话（带 memory）

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"u1001","message":"你好，先记住我叫小明"}'
```

### 5.2 文生图（qwen-image-plus）

```bash
curl -X POST http://localhost:8080/api/image/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt":"生成一张企业级数据大屏深色科技风背景"}'
```

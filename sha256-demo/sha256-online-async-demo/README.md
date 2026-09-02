# SHA-256 在线异步计算 Demo

一个可直接用 IDEA 启动的 Spring Boot + WebFlux 示例：浏览器上传文件，后端创建 SHA-256 任务，并由独立线程池异步计算。

## 技术栈

- Java 17+
- Spring Boot 3.3.5
- Spring WebFlux
- Maven
- 原生 HTML/CSS/JavaScript

## 为什么不会阻塞 HTTP 计算线程

1. 文件上传入口使用 Spring WebFlux `FilePart.transferTo(...)`，HTTP I/O 由 Reactor/Netty 管理。
2. 文件上传完成后，接口只负责把计算任务提交到专用 `sha256Executor`，然后返回 `202 Accepted + taskId`。
3. SHA-256 的磁盘读取和 MessageDigest 计算运行在独立、有限容量的工作线程池，不运行在 Netty EventLoop 上。
4. 前端通过 `GET /api/sha256/tasks/{taskId}` 轮询任务状态和计算进度。
5. 文件按 1MB 缓冲区流式计算，不会把整个大文件加载到 JVM 堆中。
6. 线程池队列有界（100），过载时快速拒绝任务，避免无限堆积导致 OOM。
7. 计算结束后立即删除上传临时文件；任务元数据保留 30 分钟后自动清理。

> 注意：客户端把文件上传到服务器这一过程本身必须传输完整请求体；这里的“不阻塞”指 WebFlux 不占用传统 Servlet 请求线程等待 I/O，并且 SHA-256 计算不会占用 Reactor/Netty EventLoop。

## IDEA 直接运行

1. IDEA 打开项目根目录。
2. 确保 Project SDK 为 Java 17 或更高。
3. 等 Maven 下载依赖。
4. 打开：

```text
src/main/java/com/example/sha256/Sha256OnlineApplication.java
```

5. 点击 `main()` 左侧绿色 Run 按钮。
6. 浏览器打开：

```text
http://localhost:8080
```

## API

### 1. 上传并创建异步任务

```http
POST /api/sha256/tasks
Content-Type: multipart/form-data
file=<文件>
```

返回 HTTP 202：

```json
{
  "taskId": "5b2558d7a71a46e88fc01e5d25a9fb73",
  "fileName": "demo.zip",
  "status": "QUEUED",
  "statusUrl": "/api/sha256/tasks/5b2558d7a71a46e88fc01e5d25a9fb73"
}
```

### 2. 查询任务

```http
GET /api/sha256/tasks/{taskId}
```

计算中：

```json
{
  "taskId": "...",
  "fileName": "demo.zip",
  "status": "RUNNING",
  "totalBytes": 104857600,
  "processedBytes": 52428800,
  "progress": 50,
  "sha256": null,
  "error": null
}
```

完成：

```json
{
  "status": "SUCCESS",
  "progress": 100,
  "sha256": "33da92f6cc1b0478f94a89df645d9e241f213107425a86dff8bb28b3bc6b8d4f"
}
```

## 并发设计

默认 worker 数：

```text
max(2, min(CPU核心数, 8))
```

任务队列：

```text
100
```

超过容量后不会让请求线程执行 SHA-256，而是快速返回任务失败状态，防止 EventLoop 被占用。

生产环境如果是多实例部署，建议把任务状态从 `ConcurrentHashMap` 替换成 Redis，并将待计算任务发送 Kafka/RabbitMQ，由独立 Worker 服务消费。

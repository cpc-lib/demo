# RAG Agent Frontend (React + Ant Design)

基于你提供的 `langchain4j-milvus-rag-demo-v2` 后端项目，我补了一套可直接联调的 React + Ant Design 前端控制台。

## 功能覆盖

- 智能问答页
  - 对接 `GET /api/chat/detail`
  - 展示答案、来源、工具调用轨迹、知识库命中状态
- 知识库导入页
  - 对接 `POST /api/ingest/text`
  - 对接 `POST /api/ingest/file`
- 向量库配置页
  - 对接 `/api/vector-stores/current`
  - 对接 `/api/vector-stores`
  - 对接 `/api/vector-stores/switch`
- Milvus 集合管理页
  - 对接 `/api/milvus/collections`
  - 对接 `/api/milvus/collections/{collectionName}`
  - 对接 `/api/milvus/collections/query`
  - 对接 `POST /api/milvus/collections`

## 技术栈

- React 18
- TypeScript
- Vite
- Ant Design 5
- Axios
- React Router

## 启动方式

### 1. 先启动后端

默认假设你的后端运行在：

```bash
http://localhost:8080
```

### 2. 安装依赖

```bash
npm install
```

### 3. 启动前端

```bash
npm run dev
```

默认访问：

```bash
http://localhost:5173
```

Vite 已配置代理：

- `/api` -> `http://localhost:8080`

## 如果后端地址不是本机 8080

复制 `.env.example` 为 `.env`，然后配置：

```bash
VITE_API_BASE_URL=http://你的后端地址:8080
```

## 页面说明

### 1. 智能问答

建议优先调试：

- `Java线程池有哪些核心参数？`
- `明天上海天气如何？`
- `OpenAI 最新发布了什么 Agent 相关能力？`

### 2. 知识库导入

- 文本导入适合快速验证切分和入库
- 文件导入适合测试 Markdown / PDF / Word 等解析流程

### 3. 向量库配置

支持保存多个 Milvus 别名配置，并动态切换当前生效配置。

### 4. Milvus 集合管理

适合做底层管理台：

- 查看集合列表
- 查看集合详情
- 创建集合
- Query 调试

## 目录结构

```text
src/
├── api/
├── components/
├── layouts/
├── pages/
├── types/
└── utils/
```

## 建议你下一步继续增强

- 接入 `/api/chat/stream`，做 SSE 打字机流式输出
- 增加登录态与路由权限
- 增加深色主题切换
- 增加聊天历史记录面板
- 增加来源过滤、工具轨迹时间线
- 增加 collection schema 可视化展示

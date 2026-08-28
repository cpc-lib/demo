# Article Delay Publish Admin UI

React + TypeScript + Ant Design 管理页面，与 Spring Boot `/api/articles` 契约对齐。

## 本地开发

先启动后端 `http://localhost:8080`，再执行：

```bash
npm install
npm run dev
```

打开 `http://localhost:5173`。

Vite 会将 `/api` 代理到后端 8080 端口，因此本地开发无需额外配置 CORS。

## 功能

- 分页文章列表
- 按标题搜索
- 按 `DRAFT / SCHEDULED / PUBLISHED` 状态过滤
- 创建草稿
- 设置定时发布
- 修改发布时间
- 取消定时发布
- 文章详情抽屉
- ProblemDetail 后端错误提示

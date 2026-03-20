# MinIO Chunk Upload Plus Backend

## 启动步骤

1. 创建数据库并执行 `src/main/resources/schema.sql`：

```sql
CREATE DATABASE minio_chunk DEFAULT CHARSET utf8mb4;
USE minio_chunk;
-- 执行 schema.sql 内容
```

2. 修改 `src/main/resources/application.yml` 中的 MySQL 和 MinIO 配置。

3. 启动后端：

```bash
mvn spring-boot:run
```

4. 前端使用 `frontend/index.html`，默认访问 `http://localhost:8080`。

接口返回的下载地址为 MinIO 的 **预签名 URL**，有效期 5 分钟，过期后自动失效。

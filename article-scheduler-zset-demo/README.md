# article-scheduler-zset-demo

Spring Boot + MyBatis-Plus + Redis **ZSet 轮询调度** 实现定时发布文章（修复了 TTL 过期监听的不可靠问题）。

## 1. 功能说明

- 文章存 MySQL（status=0 草稿, status=1 已发布）
- 创建文章时写入 Redis ZSet：
  - key: `article:schedule`
  - member: 文章 ID
  - score: 计划发布时间的时间戳（毫秒）
- 后台调度任务每秒扫描一次：
  - 找出 score <= 当前时间 的文章 ID
  - 先从 ZSet 删除，再更新 MySQL 状态为已发布（幂等）

> 不再依赖 Redis `notify-keyspace-events`，不会因为 Redis 重启 / 配置问题导致任务丢失，更适合集群和生产环境。

## 2. 数据库

执行 `sql/article.sql`：

```sql
CREATE DATABASE IF NOT EXISTS article_db;
USE article_db;
CREATE TABLE article (...);
```

## 3. 配置

修改 `src/main/resources/application.yml` 中的数据源配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/article_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: 你的用户名
    password: 你的密码
```

Redis 默认使用本机 6379 端口，如有修改，请同步调整。

## 4. 启动项目

```bash
mvn spring-boot:run
```

## 5. 接口示例

### 5.1 创建定时发布文章

- URL: `POST http://localhost:8080/articles`
- Body(JSON) 示例：

```json
{
  "title": "测试定时发布文章（ZSet）",
  "content": "内容内容内容...",
  "publishTime": "2025-12-08 15:30:00"
}
```

### 5.2 查询文章列表

```bash
GET http://localhost:8080/articles
```

### 5.3 查询单个文章

```bash
GET http://localhost:8080/articles/{id}
```

## 6. 说明

- 核心调度类：`ArticleScheduleTask`，使用 `@Scheduled(fixedRate = 1000)` 每秒轮询
- 幂等发布逻辑在 `ArticleService.publishArticle` 中实现
- 支持多节点部署：因为先 `ZREM` 再发布，只有成功删除任务的节点会发布文章

你可以在此基础上扩展：
- 取消定时发布 / 修改发布时间接口
- 发布失败重试机制
- Swagger/Knife4j 文档
- 鉴权、审核流程等

# article-scheduler-demo

Spring Boot + MyBatis-Plus + Redis TTL 实现定时发布文章的最简示例。

## 快速启动步骤

1. 导入 `sql/article.sql` 到本地 MySQL

2. 修改 `src/main/resources/application.yml` 中的数据源配置：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/article_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
       username: 你的用户名
       password: 你的密码
   ```

3. 确保 Redis 已启动，并开启 keyspace 通知（redis.conf）：
   ```conf
   notify-keyspace-events Ex
   ```

   若使用 Docker 默认配置，可在启动参数中追加：
   ```bash
   redis-server --notify-keyspace-events Ex
   ```

4. 启动项目：
   ```bash
   mvn spring-boot:run
   ```

5. 测试创建定时发布文章：

   - 请求地址：`POST http://localhost:8080/articles`
   - 请求体 JSON 例子：
     ```json
     {
       "title": "测试定时发布文章",
       "content": "内容内容内容...",
       "publishTime": "2025-12-08 15:30:00"
     }
     ```

   当达到 `publishTime` 时，会自动将文章状态从 `0` 更新为 `1`。

6. 查询文章：
   ```bash
   GET http://localhost:8080/articles
   GET http://localhost:8080/articles/{id}
   ```

> 这是一个最小可运行示例，你可以在此基础上继续扩展，如：
> - 登录鉴权
> - 多状态（待审核、已下线等）
> - 配合 MQ 做更可靠的事件通知
> - 加 Swagger / Knife4j 文档

# Versioned Cache Demo (Uber 风格版本号防止脏写)

技术栈：

- Spring Boot 2.7.18
- MySQL
- MyBatis-Plus（带乐观锁 @Version）
- Redis（StringRedisTemplate）

## 1. 数据库表

```sql
CREATE DATABASE version_cache_demo DEFAULT CHARACTER SET utf8mb4;

USE version_cache_demo;

CREATE TABLE IF NOT EXISTS article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT,
  data_version BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 2. 启动步骤

1. 修改 `src/main/resources/application.yml` 中的 MySQL、Redis 配置。
2. 执行上面的建库建表 SQL。
3. 项目根目录执行：

```bash
mvn spring-boot:run
```

## 3. 接口说明

### 新增文章

`POST http://localhost:8080/api/articles`

```json
{
  "title": "hello",
  "content": "world"
}
```

返回示例（省略部分字段）：

```json
{
  "id": 1,
  "title": "hello",
  "content": "world",
  "dataVersion": 0
}
```

### 查询文章（带缓存）

`GET http://localhost:8080/api/articles/{id}`

### 更新文章（必须带上 dataVersion 才能触发 @Version 乐观锁）

`PUT http://localhost:8080/api/articles/{id}`

```json
{
  "id": 1,
  "title": "new title",
  "content": "new content",
  "dataVersion": 0
}
```

> 注意：`dataVersion` 必须是你 **上次查询** 该记录时拿到的版本号。  
> - 如果在你提交前，另一个请求已经把这条记录更新为 `dataVersion = 1`，
> - 那么你的这次更新仍然带着旧版本 `0`，MyBatis-Plus 会根据 `WHERE id=? AND data_version=?` 检测到更新行数为 0，
> - 这时 `ArticleService.updateById(article)` 返回 `false`，`VersionedArticleCache.updateAndRefreshCache()` 会抛出 `并发更新失败，请重试`。
>
> 这样就说明 `@Version` 已经 **生效** 并且成功拦截了并发写冲突。


## 4. Uber 风格 Version-Cache

更新成功后，`VersionedArticleCache` 会：

1. 从 DB 重新读出最新的 `Article`（此时 `dataVersion` 已经自增）；
2. 写缓存时先读 Redis 的版本号 `article:ver:{id}`；
3. 如果缓存里的版本号比 DB 的还大，说明缓存已经被更新过，本次请求是“老请求”，直接丢弃这次缓存写入；
4. 否则覆盖缓存中的版本号和对象。

这样就可以避免“老请求覆盖新数据”，保证 DB 与缓存的一致性。

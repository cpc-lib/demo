# mp-typehandler-demo

基于原始 `MyBatis-Plus + TypeHandler` 示例，新增了 `EasyExcel` 导出能力，并把导出任务状态持久化到 MySQL，避免服务重启后内存任务丢失。

## 新增能力

- 导出字段由请求参数 `fields` 动态决定
- 支持两种导出模式
  - 分页导出：`pageNo > 0 && pageSize > 0`
  - 全量导出：`pageNo = -1 && pageSize = -1`
- 采用 `EasyExcel` 流式写文件
- 通过有界线程池控制导出任务队列，避免瞬时高并发造成 OOM
- 导出任务状态持久化到 `export_task` 表
- 服务重启后自动把 `WAITING/RUNNING` 任务修正为 `FAILED`
- 导出任务支持：提交、查询状态、下载文件
- 定时清理 24 小时前的导出文件，并更新任务状态为 `EXPIRED`

## 导出接口

### 1. 提交导出任务

`POST /userProfile/export/tasks`

示例：分页导出

```json
{
  "pageNo": 1,
  "pageSize": 10,
  "name": "张",
  "fields": ["id", "name", "hobbies", "tags"]
}
```

示例：全量导出

```json
{
  "pageNo": -1,
  "pageSize": -1,
  "fields": ["id", "name", "hobbies", "tags"]
}
```

### 2. 查询任务状态

`GET /userProfile/export/tasks/{taskId}`

### 3. 下载文件

`GET /userProfile/export/tasks/{taskId}/download`

## 队列控制策略

- 核心线程数：1
- 最大线程数：1
- 队列容量：2
- 数据库活动任务上限：3
- 队列满后直接拒绝，返回：`导出队列已满，请稍后再试`

这样最多同时承受：

- 1 个运行中任务
- 2 个等待中任务

## 为什么不会 OOM

- 不在 HTTP 请求线程里直接组装整个 Excel
- 导出任务进入有界线程池
- 全量导出按 `id` 游标分批查询，每批 500 条
- `EasyExcel` 流式写磁盘，不把全量数据堆在 JVM 内存里
- 导出进度实时回写到 MySQL，不依赖 JVM 内存态任务表

## 启动

1. 按 `src/main/resources/schema.sql` 初始化 MySQL
2. 修改 `application.yml` 中的数据源配置
3. 启动项目

```bash
mvn clean package
java -jar target/mp-typehandler-demo-1.1.0.jar
```


## 定时清理任务

项目已显式启用 Spring Scheduling，并新增独立任务类 `cc.ivera.job.ExportTaskCleanupJob`。

- 默认 cron：`0 0 * * * ?`，即每小时执行一次
- 清理逻辑入口：`UserProfileExportService.cleanupExpiredTasks()`
- 只清理状态为 `SUCCESS` 且 `expire_at < now()` 的任务
- 删除物理文件后，将任务状态更新为 `EXPIRED`，并清空 `file_path`

可通过 `application.yml` 调整：

```yaml
export:
  cleanup:
    cron: 0 0 * * * ?
  file:
    expire-hours: 24
```

# mp-typehandler-demo-v3

基于 Spring Boot 3 + MyBatis-Plus + MySQL + EasyExcel 的示例工程。

## 本版新增

### 1. AOP + 注解方式记录修改日志
- 使用 `@ChangeLog` 注解声明修改日志能力
- 使用 `ChangeLogAspect` 切面统一采集修改前后快照并入库
- Service 不再直接拼装变更日志，业务更新逻辑和审计逻辑解耦
- 支持两种模式：
  - `ANNOTATION`：从实体字段上的 `@LogField` 自动识别需跟踪字段
  - `HARDCODED`：在 `@ChangeLog(hardcodedFields=...)` 中硬编码指定字段
- 使用 `ChangeLogSnapshotProvider` 扩展不同实体的快照加载方式

### 2. 导出能力
- EasyExcel 动态字段导出
- `pageNo = -1` 且 `pageSize = -1` 导出全部数据
- MySQL 持久化导出任务状态
- 定时清理过期文件
- 正确返回 `.xlsx` 下载头

## 修改日志设计说明

### AOP 设计
1. Controller 调用 Service 更新方法。
2. Service 更新方法上加 `@ChangeLog`。
3. `ChangeLogAspect` 在方法执行前通过 `ChangeLogSnapshotProvider` 读取旧数据快照。
4. 方法执行成功后再次读取新数据快照。
5. 根据注解配置选择字段来源：
   - `ANNOTATION`：实体字段上的 `@LogField`
   - `HARDCODED`：注解 `hardcodedFields = {"name:姓名", ...}`
6. 统一调用 `ChangeLogService` 将主表与明细表落库。

### 表设计
- `change_log`：存放一次更新行为的整体信息
- `change_log_detail`：存放每个字段的变更明细

这样做的优点是：
- 业务更新与日志记录解耦
- 可复用到更多实体
- 便于后续扩展审计人、来源系统、租户等字段

## 启动
1. 执行 `src/main/resources/schema.sql`
2. 修改 `application.yml` 数据库连接
3. 运行 `mvn clean package` 或直接启动 `DemoApplication`

## 更新接口示例

### 注解模式
```http
PUT /userProfile/1
X-Operator: admin
Content-Type: application/json

{
  "name": "李四",
  "hobbies": ["游泳", "羽毛球"],
  "tags": [{"id": 1, "name": "Java"}]
}
```

### 硬编码模式
```http
PUT /userProfile/1/hardcoded
X-Operator: admin
Content-Type: application/json

{
  "name": "王五"
}
```

### 查询变更日志
```http
GET /userProfile/1/change-logs
```

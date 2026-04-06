# mp-typehandler-demo-v2

基于 Spring Boot 3 + MyBatis-Plus + MySQL + EasyExcel 的示例工程。

## 本版新增

### 1. 更新接口字段变更日志
- 支持注解方式记录字段变更：`PUT /userProfile/{id}`
- 支持硬编码方式记录字段变更：`PUT /userProfile/{id}/hardcoded`
- 变更日志采用主表 + 明细表设计：`change_log`、`change_log_detail`
- 可查询指定用户的变更记录：`GET /userProfile/{id}/change-logs`

### 2. 导出能力
- EasyExcel 动态字段导出
- `pageNo = -1` 且 `pageSize = -1` 导出全部数据
- MySQL 持久化导出任务状态
- 定时清理过期文件
- 正确返回 `.xlsx` 下载头

## 字段变更日志设计说明

### 注解方式
在 `UserProfile` 字段上通过 `@LogField(label = "字段名")` 声明可记录字段。

### 硬编码方式
在 `UserProfileService#updateWithHardcoded` 中通过 `TrackedFieldDefinition` 明确指定要跟踪的字段。

### 表设计
- `change_log`：存放一次更新行为的整体信息
- `change_log_detail`：存放每个字段的变更明细

这样做的优点是：
- 便于按业务维度检索
- 便于后续扩展审计人、租户、来源系统
- 适合一个更新操作对应多个字段变更的场景

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

# Spring Boot + MyBatis-Plus + MySQL TypeHandler Demo

本项目演示：

- `JacksonTypeHandler`：将 `List<TagItem>` 映射到 MySQL `JSON` 字段
- 自定义 `ListStringTypeHandler`：将 `List<String>` 映射到 MySQL `JSON` 字段

## 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.x

## 2. 初始化数据库

执行 `src/main/resources/schema.sql`

默认库名：`mp_demo`
默认表名：`user_profile`

## 3. 修改数据库连接

编辑 `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mp_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 123456
```

## 4. 启动项目

```bash
mvn spring-boot:run
```

或先打包：

```bash
mvn clean package
java -jar target/mp-mysql-typehandler-demo-1.0.0.jar
```

## 5. 接口测试

### 5.1 新增默认示例数据

```bash
curl -X POST http://localhost:8080/userProfile/add
```

### 5.2 查询

```bash
curl http://localhost:8080/userProfile/1
```

### 5.3 更新

```bash
curl -X PUT http://localhost:8080/userProfile/1
```

### 5.4 自定义新增

```bash
curl -X POST http://localhost:8080/userProfile/custom \
  -H "Content-Type: application/json" \
  -d '{
    "name": "李四",
    "hobbies": ["羽毛球", "摄影"],
    "tags": [
      {"id": 100, "name": "MySQL"},
      {"id": 101, "name": "JSON"}
    ]
  }'
```

## 6. 核心说明

实体类必须加：

```java
@TableName(value = "user_profile", autoResultMap = true)
```

否则字段上的 `typeHandler` 不会按预期生效。

字段示例：

```java
@TableField(typeHandler = ListStringTypeHandler.class)
private List<String> hobbies;

@TableField(typeHandler = JacksonTypeHandler.class)
private List<TagItem> tags;
```

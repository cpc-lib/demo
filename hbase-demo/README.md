# Student HBase CRUD Demo (Spring Boot 2.7.8 + Java 17 + HBase 2.5)

This demo provides:
- Student CRUD via HBase table `demo:student`
- Cursor-based pagination: `/api/students?pageSize=10&cursor=<rowkey>`
- Docker Compose for local HBase 2.5 standalone

## 1) Start HBase

```bash
docker-compose up -d
```

HBase master UI: http://localhost:16010

ZooKeeper is mapped to host port **2182**.

## 2) Run App

```bash
mvn -q -DskipTests package
java -jar target/student-hbase-crud-demo-1.0.0.jar
```

On startup it will create namespace `demo` and table `demo:student` automatically.

## 3) APIs

### Create
POST `http://localhost:8080/api/students`

```json
{"id":"S001","name":"Alice","age":18,"grade":"G12"}
```

### Get
GET `/api/students/S001`

### Update
PUT `/api/students/S001`

```json
{"name":"Alice B","age":19,"grade":"G12"}
```

### Delete
DELETE `/api/students/S001`

### List (cursor pagination)
GET `/api/students?pageSize=2`

Response:
```json
{
  "items":[...],
  "nextCursor":"stu#S001"
}
```

Next page:
GET `/api/students?pageSize=2&cursor=stu%23S001`

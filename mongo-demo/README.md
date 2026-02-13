# springboot-mongo-crud (Full)

Spring Boot **2.7.8** + Java **8** + MongoDB Replica Set (**rs0**) with **--auth**.

Features:
- CRUD APIs
- **Production-grade paging** (MongoTemplate dynamic criteria, safe sort whitelist, max page size)
- Optional filters (uid/phone/email/nickname/status/date range)
- Consistent API response wrapper
- Global exception handling

## Run

1) Edit `application.yml` MongoDB URI if needed.

2) Start app:
```bash
mvn spring-boot:run
```

## APIs

### Create
POST `/api/users`
```json
{"uid":"u1001","phone":"13800000000","email":"u1001@test.com","nickname":"Tom","status":1}
```

### Get
GET `/api/users/{id}`

### Update
PUT `/api/users/{id}`
```json
{"nickname":"Tommy","status":1}
```

### Delete
DELETE `/api/users/{id}`

### Page Query
GET `/api/users?page=0&size=10&keyword=tom&status=1&sort=updatedAt,desc&createdFrom=2026-01-01T00:00:00Z&createdTo=2026-01-10T00:00:00Z`

- `page` zero-based
- `size` capped by `app.paging.max-page-size`
- `sort` supports: `createdAt`, `updatedAt`, `uid`, `email`, `phone`, `nickname`, `status`.
  - format: `field,asc|desc` (multiple sort items separated by `;`), e.g. `updatedAt,desc;uid,asc`
- `keyword` searches `uid/phone/email/nickname` (case-insensitive for nickname/email)

## Notes

- Indexes are declared via annotations (uid unique, phone/email normal, compound indexes).
- For real production, you may:
  - add field encryption / masking
  - add optimistic locking (version)
  - add soft delete
  - add audit fields with Mongo Auditing

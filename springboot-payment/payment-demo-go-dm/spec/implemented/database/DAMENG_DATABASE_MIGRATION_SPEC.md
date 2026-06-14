# 达梦数据库迁移 SPEC

## 0. 元信息

- **状态**：implemented
- **日期**：2026-06-13
- **领域**：database | backend | config | docs | tests
- **变更类型**：设计变更、公共配置兼容影响、数据库方言迁移
- **目标**：将 Go 后端默认数据库从 MySQL 切换为国产达梦数据库 DM8。

## 1. 背景

当前 `payment-demo-go/` 使用 `gorm.io/driver/mysql` 连接 MySQL，`config/application.yml` 使用 `jdbc:mysql://...`，初始化脚本 `sql/payment_demo.sql` 使用 MySQL 方言，包括反引号、`AUTO_INCREMENT`、`ENGINE`、`CHARACTER SET`、`COLLATE`、`ON UPDATE CURRENT_TIMESTAMP`、`SET FOREIGN_KEY_CHECKS` 等。

本次迁移要求把运行时数据库改为达梦数据库，保持既有 HTTP API、支付流程、订单/退款状态、Redis、RabbitMQ、前端调用契约不变。

## 2. 目标行为

### 2.1 后端连接

- Go 后端使用达梦 GORM 方言连接数据库。
- `internal/db.Open` 使用达梦 dialector，不再依赖 MySQL dialector。
- 启动日志和错误信息使用“达梦数据库”或“数据库”，不再写死 MySQL。

### 2.2 配置

- `Config` 中数据库配置语义改为通用 `Database`，运行时目标为达梦。
- `application.yml` 默认数据源改为达梦：
  - `driver-class-name: dm.jdbc.driver.DmDriver`
  - `url: jdbc:dm://...`
  - `username` / `password` 保持独立字段。
- Go 配置加载需要把 `jdbc:dm://host:port?schema=PAYMENT_DEMO&...` 转为达梦 Go DSN：
  - `dm://username:password@host:port?schema=PAYMENT_DEMO&...`
- `username` / `password` 会按达梦 Go driver 的解析规则原样拼入 DSN，避免 `#`、`@` 等密码字符被 URL 编码后导致登录失败。
- 兼容裸达梦 DSN：
  - 输入 `dm://user:password@host:port?schema=PAYMENT_DEMO` 时保持可用。
- 不再把 MySQL JDBC URL 作为默认成功路径；非法或空 URL fallback 到本地达梦：
  - `dm://SYSDBA:SYSDBA@127.0.0.1:5236?schema=PAYMENT_DEMO`

### 2.3 初始化 SQL

- `payment-demo-go/sql/payment_demo.sql` 改为达梦可执行脚本。
- 初始化脚本负责创建并切换到 `PAYMENT_DEMO` schema，使其与 `spring.datasource.url` 中的 `schema=PAYMENT_DEMO` 保持一致。
- 表名、字段名、索引名、种子支付应用、订单绑定字段保持现有业务含义。
- 数据库字段 `t_order_info.payment_app_id` 继续 nullable。
- MySQL 方言语法不得残留在初始化脚本中。
- `update_time` 不再依赖 MySQL `ON UPDATE CURRENT_TIMESTAMP`，由 GORM `autoUpdateTime` 负责更新。

### 2.4 SQL 方言

- 服务层通用 SQL 表达式优先使用标准 SQL。
- 当前 `ifnull(sum(refund),0)` 改为 `COALESCE(sum(refund),0)`。
- GORM 行锁、事务、唯一索引、状态条件更新的业务语义不变。

## 3. 非目标

- 不新增数据库迁移工具。
- 不改 HTTP 路径、请求字段、响应结构、状态值、MQ 名称、Redis key。
- 不改前端调用逻辑。
- 不要求同一运行时同时支持 MySQL 和达梦。
- 不引入真实达梦实例作为自动化测试依赖。

## 4. 兼容影响

- **DB 兼容**：初始化脚本从 MySQL 方言切换为达梦方言，旧 MySQL 实例不能继续直接执行该脚本。
- **配置兼容**：`spring.datasource.url` 默认改为 `jdbc:dm://...`，旧 `jdbc:mysql://...` 不再作为目标配置。
- **依赖兼容**：Go module 移除 MySQL GORM driver，增加达梦 GORM driver。
- **数据兼容**：表结构和业务字段保持同名，历史数据迁移需要由运维单独导出导入，不在本 spec 范围内。

## 5. 验收标准

- [x] 配置测试证明达梦 JDBC URL 会被转换为 `dm://...` DSN。
- [x] 数据库 dialector 测试证明后端使用达梦 GORM 方言。
- [x] SQL 特征测试证明初始化脚本会创建并切换到 `PAYMENT_DEMO` schema。
- [x] SQL 特征测试证明初始化脚本使用达梦语法，并拒绝 MySQL 专属语法残留。
- [x] 服务层测试或特征测试证明退款金额聚合使用标准 `COALESCE` 表达式。
- [x] `application.yml`、`README.md`、`CODE_INTRO.md` 和当前行为 spec 不再把后端运行数据库描述为 MySQL。
- [x] `cd payment-demo-go && go test ./...` 通过。

## 6. 实现锚点

- `payment-demo-go/go.mod`
- `payment-demo-go/internal/config/config.go`
- `payment-demo-go/internal/config/config_characterization_test.go`
- `payment-demo-go/internal/db/db.go`
- `payment-demo-go/internal/db/*_test.go`
- `payment-demo-go/internal/service/refund_status.go`
- `payment-demo-go/sql/payment_demo.sql`
- `payment-demo-go/sql/payment_demo_sql_characterization_test.go`
- `payment-demo-go/config/application.yml`
- `payment-demo-go/README.md`
- `CODE_INTRO.md`
- `spec/README.md`
- `spec/implemented/current-behavior/PAYMENT_DEMO_GO_CURRENT_BEHAVIOR_SPEC.md`

## 7. 测试计划

1. 先补失败测试：
   - `Load` 读取达梦 datasource 并生成正确 DSN。
   - `db.Dialector` 返回 name 为 `dm`。
   - SQL 脚本包含达梦 identity / clob / 普通索引语法。
   - SQL 脚本不包含 MySQL 专属语法。
   - 退款聚合 SQL 不再使用 `ifnull`。
2. 再实现最小代码改动：
   - 切换 GORM driver。
   - 重命名配置语义。
   - 转换 DSN。
   - 改 SQL 脚本。
   - 更新文档。
3. 最后运行 `go test ./...` 并按结果修正。

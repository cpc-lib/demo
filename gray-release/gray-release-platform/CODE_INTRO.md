# CODE_INTRO - 灰度发布管理平台 代码介绍

## 1. 项目概述

灰度发布管理平台是一套完整的、可本地运行的灰度发布管理系统 Demo，覆盖灰度规则管理、发布任务编排、审批流程、蓝绿部署、A/B 测试、自动回滚、监控告警等全链路功能。

- **技术栈**: Java 17 + Spring Boot 3.3 + Spring Cloud Alibaba 2023 + MyBatis-Plus 3.5 + MySQL 8 + Redis + Nacos + Sentinel + Prometheus + Grafana + React 18 + Vite + Ant Design 5
- **构建工具**: Maven (多模块) + Vite
- **容器化**: Docker Compose (基础设施 + 业务服务)
- **编排**: Kubernetes / Istio (生产部署)

---

## 2. 项目结构

```
gray-release-platform/
├── pom.xml                          # 根 POM，聚合 backend 模块
├── backend/
│   ├── pom.xml                      # 后端父 POM，依赖管理 (Spring Boot/Cloud/Alibaba/MyBatis-Plus)
│   ├── gray-common/                 # 公共模块 - DTO、枚举、工具类
│   │   └── src/main/java/com/example/gray/common/
│   │       ├── ApiResponse.java         # 统一 API 响应包装
│   │       ├── GrayEnums.java           # 枚举定义 (RuleType/ReleaseStrategy/ReleaseStatus/ApprovalStatus/AlertLevel)
│   │       ├── GrayMatchRequest.java    # 灰度匹配请求 DTO
│   │       ├── GrayMatchResult.java     # 灰度匹配结果 DTO
│   │       └── VersionCompare.java      # 版本号比较工具
│   ├── gray-admin/                  # 管理端 - 核心业务模块
│   │   ├── src/main/java/com/example/gray/admin/
│   │   │   ├── GrayAdminApplication.java  # 启动类 (@MapperScan, @EnableScheduling)
│   │   │   ├── config/
│   │   │   │   ├── WebConfig.java         # CORS 跨域配置
│   │   │   │   └── TraceIdFilter.java     # TraceId 透传过滤器
│   │   │   ├── security/                  # JWT 认证与 RBAC 鉴权
│   │   │   │   ├── AuthRequest.java       # 登录请求
│   │   │   │   ├── AuthResponse.java      # 登录响应 (含 JWT + 角色)
│   │   │   │   ├── JwtService.java        # JWT 签发与验证
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT 过滤器
│   │   │   │   ├── SecurityConfig.java    # Spring Security 配置 (RBAC)
│   │   │   │   ├── SecurityUtil.java      # 安全工具
│   │   │   │   └── TokenPrincipal.java    # Token 主体
│   │   │   ├── entity/                    # 实体类 (MyBatis-Plus @TableName)
│   │   │   │   ├── GrayRule.java          # 灰度规则
│   │   │   │   ├── ReleaseTask.java       # 发布任务
│   │   │   │   ├── ReleaseApproval.java   # 发布审批
│   │   │   │   ├── ServicePolicy.java     # 服务策略 (蓝绿/AB)
│   │   │   │   ├── AbMetric.java          # A/B 测试指标
│   │   │   │   ├── AlertEvent.java        # 告警事件
│   │   │   │   └── AuditLog.java          # 审计日志
│   │   │   ├── mapper/                    # MyBatis-Plus Mapper 接口
│   │   │   │   ├── GrayRuleMapper.java
│   │   │   │   ├── ReleaseTaskMapper.java
│   │   │   │   ├── ReleaseApprovalMapper.java
│   │   │   │   ├── ServicePolicyMapper.java
│   │   │   │   ├── AbMetricMapper.java
│   │   │   │   ├── AlertEventMapper.java
│   │   │   │   └── AuditLogMapper.java
│   │   │   ├── service/                   # 业务服务层
│   │   │   │   ├── GrayRuleService.java        # 灰度规则 CRUD + 匹配引擎 + Redis 缓存 + Nacos 发布
│   │   │   │   ├── ReleaseTaskService.java     # 发布任务生命周期 + 自动回滚 + 阶段推进
│   │   │   │   ├── ReleaseApprovalService.java # 审批流程
│   │   │   │   ├── ServicePolicyService.java   # 蓝绿切流 + A/B 配置
│   │   │   │   ├── AbMetricService.java        # A/B 曝光转化记录
│   │   │   │   ├── AlertService.java           # 告警创建 + Webhook 外发
│   │   │   │   ├── AuditService.java           # 审计日志记录
│   │   │   │   └── NacosRulePublisher.java     # Nacos 配置发布
│   │   │   ├── controller/                # REST 控制器
│   │   │   │   ├── AuthController.java         # POST /api/auth/login
│   │   │   │   ├── GrayRuleController.java     # CRUD /api/rules
│   │   │   │   ├── ReleaseTaskController.java  # CRUD + 状态变更 /api/releases
│   │   │   │   ├── ReleaseApprovalController.java  # 审批操作 /api/approvals
│   │   │   │   ├── InternalMatchController.java    # 内部匹配 (网关调用) /api/internal/match
│   │   │   │   ├── DiagnosisController.java        # 灰度诊断 /api/diagnosis/match
│   │   │   │   ├── DashboardController.java        # 大盘统计 /api/dashboard/summary
│   │   │   │   ├── AlertController.java            # 告警查询 /api/alerts
│   │   │   │   ├── AbMetricController.java         # A/B 指标 /api/ab-metrics
│   │   │   │   ├── ServicePolicyController.java    # 服务策略 /api/policies
│   │   │   │   └── GlobalExceptionHandler.java     # 全局异常处理
│   │   │   └── resources/
│   │   │       ├── application.yml            # 应用配置
│   │   │       └── db/migration/
│   │   │           └── V1__init_gray_release.sql   # Flyway 数据库迁移
│   │   ├── src/test/java/com/example/gray/admin/
│   │   │   ├── GrayRuleServiceTest.java
│   │   │   ├── ReleaseTaskServiceTest.java
│   │   │   ├── JwtServiceTest.java
│   │   │   └── ControllerParameterMetadataTest.java
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── gray-gateway/                  # 灰度网关 - 流量路由入口
│   │   ├── src/main/java/com/example/gray/gateway/
│   │   │   ├── GrayGatewayApplication.java  # 启动类 (WebFlux)
│   │   │   └── ProxyController.java         # 核心代理逻辑
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── demo-order-v1/                 # 示例订单服务 v1 (稳定版)
│   │   ├── src/main/java/com/example/gray/demo/v1/
│   │   │   ├── DemoOrderV1Application.java
│   │   │   └── OrderController.java         # GET /api/order/health, /api/order/orders
│   │   ├── src/main/resources/application.yml
│   │   ├── Dockerfile
│   │   └── pom.xml
│   └── demo-order-v2/                 # 示例订单服务 v2 (灰度版/新功能)
│       ├── src/main/java/com/example/gray/demo/v2/
│       │   ├── DemoOrderV2Application.java
│       │   └── OrderController.java         # 同路径，返回 version:v2
│       ├── src/main/resources/application.yml
│       ├── Dockerfile
│       └── pom.xml
├── frontend/                          # React 管理后台
│   ├── src/
│   │   ├── main.jsx                  # 单文件 SPA (含所有页面组件 + Zustand 状态管理)
│   │   ├── ruleEditor.js             # 灰度规则编辑器工具函数
│   │   ├── ruleEditor.test.js        # 规则编辑器单元测试
│   │   └── styles.css                # 全局样式
│   ├── index.html
│   ├── vite.config.js                # Vite 构建配置
│   ├── nginx.conf                    # Nginx 静态文件服务 + API 反向代理
│   ├── Dockerfile
│   └── package.json
├── env/                               # 本地开发环境
│   ├── docker-compose.yml            # MySQL + Redis + Nacos + Sentinel + Prometheus + Grafana
│   └── docker/
│       ├── mysql/init/001_schema.sql  # MySQL 初始化脚本
│       ├── prometheus/prometheus.yml  # Prometheus 抓取配置
│       └── grafana/
│           ├── provisioning/         # Grafana 自动导入数据源和仪表盘
│           └── dashboards/gray-release-overview.json
└── deploy/                            # 生产部署
    ├── k8s/
    │   ├── namespace.yaml            # Kubernetes 命名空间
    │   ├── demo-order.yaml           # Deployment + Service
    │   └── istio-gray-route.yaml     # Istio DestinationRule + VirtualService
    └── postman/
        └── gray-release-platform.postman_collection.json
```

---

## 3. 核心模块详解

### 3.1 gray-common (公共模块)

纯 POJO 模块，无任何外部依赖，被所有其他模块引用。

| 类 | 说明 |
|---|---|
| `ApiResponse<T>` | 统一响应格式 `{ success, message, data }` |
| `GrayEnums` | 枚举: `RuleType`(USER/TENANT/HEADER/COOKIE/IP/APP_VERSION/REGION/PERCENT), `ReleaseStrategy`(CANARY/BLUE_GREEN/AB_TEST), `ReleaseStatus`, `ApprovalStatus`, `AlertLevel` |
| `GrayMatchRequest` | 灰度匹配请求，携带 serviceId、userId、tenantId、ip、appVersion、region、headers、cookies |
| `GrayMatchResult` | 匹配结果，包含 targetVersion、matched、ruleId、ruleName、reason |
| `VersionCompare` | 版本号语义比较工具 |

### 3.2 gray-admin (管理端)

核心业务模块，包含所有管理功能和内部匹配 API。

#### 3.2.1 数据模型 (7 张表)

| 表 | 说明 | 关键字段 |
|---|---|---|
| `gray_rule` | 灰度规则 | rule_name, service_id, target_version, rule_type, condition_key/value, traffic_percent, priority, enabled |
| `release_task` | 发布任务 | task_name, service_id, from/to_version, strategy, current_percent, status, auto_rollback, error_rate/p99 阈值 |
| `release_approval` | 发布审批 | task_id, applicant, approver, status, comment |
| `service_policy` | 服务策略 | default_version, blue/green_version, active_color, ab_enabled, ab_percent_b |
| `ab_metric` | A/B 指标 | service_id, experiment_key, variant, exposures, conversions |
| `alert_event` | 告警事件 | level(INFO/WARN/CRITICAL), source, title, content, handled |
| `audit_log` | 审计日志 | operator, action, resource_type/id, before/after_data |

#### 3.2.2 灰度规则匹配引擎 (GrayRuleService)

```
match(request) 流程:
  1. 从 Redis 缓存读取启用规则列表 (缓存 key: gray:rules:{serviceId}, TTL: 5min)
  2. 缓存未命中则从 MySQL 查询，按 priority ASC + update_time DESC 排序
  3. 遍历规则，按规则类型匹配请求上下文:
     - USER:    request.userId == rule.conditionValue
     - TENANT:  request.tenantId == rule.conditionValue
     - HEADER:  request.headers[rule.conditionKey] == rule.conditionValue
     - COOKIE:  request.cookies[rule.conditionKey] == rule.conditionValue
     - IP:      request.ip 匹配 CIDR
     - APP_VERSION: 版本号范围比较
     - REGION:  request.region == rule.conditionValue
     - PERCENT: hash(userId) % 100 < trafficPercent
  4. 命中则返回 targetVersion
  5. 未命中则使用 ServicePolicy.defaultVersion 作为兜底
```

#### 3.2.3 发布任务生命周期

```
WAITING_APPROVAL → (审批通过) → DRAFT → (启动) → RUNNING → (完成) → COMPLETED
                                      ↓                    ↓
                                  (驳回) → REJECTED    (异常) → ROLLED_BACK
                                      ↓
                                  (暂停) → PAUSED → (恢复) → RUNNING
```

- **自动回滚**: `ReleaseTaskService.reportMetrics()` 接收错误率和 P99 延迟，超阈值自动触发回滚
- **阶段推进**: `@Scheduled` 定时任务每 30s 检查 HEALTHY 状态的 RUNNING 任务，自动推进到下一阶段 (stagesJson: [1,5,20,50,100])
- **蓝绿切流**: 完成任务时自动调用 `ServicePolicyService.blueGreenSwitch()` 切换默认版本

#### 3.2.4 安全模型 (JWT + RBAC)

| 角色 | 权限 |
|---|---|
| `ADMIN` | 全部操作 |
| `RELEASE_MANAGER` | 读写 (创建规则、管理发布、审批) |
| `VIEWER` | 只读 (GET 请求) |

- `/api/auth/login` 和 `/api/internal/**` 无需认证
- GET 请求对 VIEWER 开放
- 写操作需要 ADMIN 或 RELEASE_MANAGER 角色

### 3.3 gray-gateway (灰度网关)

基于 Spring WebFlux 的代理网关，是流量的统一入口。

```
请求流程:
  Client → gray-gateway:18000/api/order/**
    1. 提取请求上下文 (X-User-Id, X-Tenant-Id, X-App-Version, X-Region, IP, Headers, Cookies)
    2. POST /api/internal/match 到 gray-admin 获取目标版本
    3. 根据目标版本转发到 demo-order-v1:18081 或 demo-order-v2:18082
    4. 透传 X-Trace-Id, X-Gray-Version, X-Gray-Rule
    5. 匹配失败时默认转发到 v1
```

### 3.4 demo-order-v1 / demo-order-v2 (示例服务)

两个结构完全相同的 Spring Boot 服务，暴露相同的 API 路径 `/api/order/health` 和 `/api/order/orders`，区别在于返回的 `version` 字段分别为 `v1` 和 `v2`，用于验证灰度路由是否生效。

### 3.5 frontend (前端管理后台)

单文件 SPA 架构 (`main.jsx`)，使用 Zustand 做状态管理。

**页面功能**:
- **登录页**: JWT 认证
- **大盘**: 规则总数、运行中任务、最近审计日志、告警列表
- **规则管理**: 增删改查 + 规则冲突检测 + 发布到 Nacos
- **发布任务**: 创建/启动/暂停/完成/回滚/推进 + 健康指标上报
- **审批流**: 待审批列表 + 通过/驳回
- **蓝绿/A-B**: 蓝绿切流 + A/B 测试开关和配置
- **诊断**: 输入条件查看命中版本和规则
- **告警**: 最近告警事件列表

---

## 4. 数据流架构

```
┌──────────┐     ┌─────────────┐     ┌──────────────┐
│  Browser  │────▶│ gray-gateway │────▶│  gray-admin   │
│ (前端后台) │     │  (18000)     │     │   (18080)     │
└──────────┘     │             │     │              │
                 │ POST /api/  │     │ POST /api/   │
                 │ internal/   │     │ internal/    │
                 │ match       │     │ match        │
                 └──────┬──────┘     └──────┬───────┘
                        │                   │
              ┌─────────▼─────────┐        │
              │  demo-order-v1    │        │
              │  (18081)          │◀───────┘
              └───────────────────┘
              ┌───────────────────┐
              │  demo-order-v2    │
              │  (18082)          │
              └───────────────────┘
```

---

## 5. 基础设施依赖

| 组件 | 端口 | 用途 |
|---|---|---|
| MySQL 8 | 3306 | 规则、任务、审批、审计、策略、指标、告警持久化 |
| Redis 7 | 6379 | 灰度规则热缓存 (5min TTL) |
| Nacos 2.3 | 8848 | 服务注册发现 + 灰度规则配置快照 |
| Sentinel 1.8 | 8858 | 网关限流熔断 |
| Prometheus | 19090 | 指标采集 (各服务 /actuator/prometheus) |
| Grafana 11 | 13000 | 可视化仪表盘 |

---

## 6. 部署架构

- **本地开发**: `env/docker-compose.yml` 启动基础设施，业务服务本地 IDE 运行
- **Docker Compose 全量**: 取消 docker-compose.yml 中业务服务注释，一键启动全部
- **Kubernetes/Istio**: `deploy/k8s/` 提供 Deployment、Service、VirtualService、DestinationRule，实现生产级灰度路由

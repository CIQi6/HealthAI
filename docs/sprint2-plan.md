# Sprint 2 设计草案：Refresh Token、审计日志、秘钥管理

## 1. 目标概述
- 巩固认证链路的安全性与可恢复性。
- 提供审计可追踪能力，满足合规要求。
- 在生产环境前落实秘钥安全存储与轮换策略。

## 2. Refresh Token 方案
- **Token 策略**：
  - Access Token（JWT）维持 60 分钟有效期。
  - 新增 Refresh Token（随机 UUID），默认 14 天有效，可配置。
  - 每次刷新时旋转 Refresh Token，旧 Token 立即失效。
- **存储设计**：
  - 使用 Redis 保存 `refresh_token:{userId}:{tokenId}` → `{"username":"xxx","expiresAt":...}`。
  - 设置 Redis TTL 与双写校验（TTL 与 `expiresAt` 字段一致）。
  - 提供黑名单撤销能力：用户登出或密码重置时删除对应键。
- **接口调整**：
  - `POST /api/v1/auth/login` 返回 `{accessToken, refreshToken, expiresIn}`。
  - 新增 `POST /api/v1/auth/refresh`，接收 Refresh Token，验证后签发新的 Access/Refresh Token 对。
  - `POST /api/v1/auth/logout` 接受 Refresh Token，清理 Redis 中存储。
- **服务改动**：
  - 扩展 `JwtProvider` 支持生成短期 Access Token 与验证操作。
  - 新增 `RefreshTokenService`，封装 Redis 操作、生成、验证与旋转逻辑。
- **异常处理**：
  - 定义 `ErrorCode.REFRESH_TOKEN_INVALID`、`REFRESH_TOKEN_EXPIRED`。
  - 记录 Refresh Token 使用失败的审计事件。

## 3. 审计日志方案
- **事件模型**：
  - 新建实体 `AuditEvent`（字段：`id`、`occurredAt`、`actorId`、`actorType`、`action`、`resourceType`、`resourceId`、`sourceIp`、`metadata`）。
  - 添加表 `audit_events`，采用 JSON(N) 存储扩展字段。
- **服务封装**：
  - 新增 `AuditTrailService` 提供 `record(AuditEvent)` 方法，使用异步队列或事务后事件。
  - 初期采用同步写入 MySQL，后续可扩展到 Kafka + 异步处理。
- **埋点范围（Sprint 2）**：
  - 用户注册、登录、刷新令牌、登出。
  - 健康档案创建、更新、删除。
  - 错误分支（如 Refresh Token 验证失败）。
- **API 支持**：
  - 预留 `/api/v1/audit-events` 管理端查询接口，Sprint 3+ 实现。
- **技术选型**：
  - 使用 Spring AOP 或事件发布器在服务方法结束时记录日志。
  - 引入 `@Audit(action="...")` 自定义注解辅助声明式埋点（可选）。

## 4. 秘钥管理方案
- **环境划分**：
  - Dev/QA：继续使用 `.env` 注入的对称秘钥，配合随机生成脚本。
  - Prod：依赖云 KMS/Secret Manager（AWS Secrets Manager、Azure Key Vault 或 HashiCorp Vault）。
- **加载流程**：
  - 启动时通过 `SecretManagerClient` 拉取 `JWT_SECRET`、数据库凭证，注入 Spring Environment。
  - 无法获取秘钥时阻止应用启动并输出明确日志。
- **轮换策略**：
  - 支持配置主备秘钥：`healthai.security.jwt.secret-primary`、`...secret-secondary`。
  - `JwtProvider` 支持读取多秘钥列表解析 Token（兼容旧 Token），签发使用主秘钥。
- **运维协同**：
  - 制定轮换 Runbook，明确触发条件、执行步骤、回滚方案。
  - 在 CI/CD 中避免秘钥曝光（禁止写入镜像层或日志）。

## 5. Sprint 2 任务拆解
- **Refresh Token**
  - ✅ 设计 Redis Key 结构与过期策略（`auth:refresh:{token}` + TTL，同步到 `RefreshTokenService`）。
  - ✅ 实现 `RefreshTokenService` + Redis 配置，提供内存降级与失效回收。
  - ✅ 扩展 `AuthController`、`AuthService`、`JwtProvider`，支持登录/刷新/登出全链路。
  - ✅ 编写集成测试覆盖登录、刷新、登出、失效场景（参见 `AuthControllerTest#shouldRefreshToken`、`#shouldRevokeRefreshTokenOnLogout`）。
- **审计日志**
  - ✅ 创建 Flyway 脚本 `V3__create_audit_events_table.sql` 并完成迁移。
  - ✅ 在 `AuthService`、`HealthProfileService` 埋点核心操作与异常分支。
  - ⏳ 增加测试验证日志写入（待补充基于 `JdbcTemplate` 的断言或 Mapper 查询，已转入 Sprint 3 Backlog）。
- **秘钥管理**
  - ✅ 引入配置结构支持主备秘钥（`JwtProperties` 与 `JwtSecretResolver`）。
  - ✅ 封装 Secret 加载接口（`SecretManagerClient` + `LocalSecretManagerClient`）。
  - ⏳ 更新 `application.properties` 与文档说明，补充生产 Secret Manager 对接示例（已转入 Sprint 3 文档任务）。
  - ⏳ 输出秘钥轮换操作手册草稿，与运维协同中（已转入 Sprint 3 文档任务）。

## 6. 未决事项
- 与运维确认目标环境 Secret 管理服务选型。
- 是否需要在 Sprint 2 内完成审计日志查询接口或推迟至 Sprint 3。
- 评估 Redis 高可用部署计划，确保 Refresh Token 持久性。

# HealthAi Backend

## 概述
HealthAi 是一个基于 Spring Boot 的医疗健康后台系统，提供账号、健康档案、生理指标、问诊、药品库、处方与通知等模块。当前阶段进入 Sprint 2，目标是加固认证安全能力并完善运维支撑。

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Security / Spring Validation / Actuator
- MyBatis + MySQL
- Redis / Kafka（通过 Docker Compose 提供）
- SpringDoc OpenAPI 3

## 当前迭代进度（Sprint 2）
- **认证链路**：`AuthService`、`AuthController` 支持注册、登录、Token 续期与登出；集成测试覆盖核心场景。
- **Refresh Token 服务**：`RefreshTokenService` 基于 Redis 存储刷新令牌，并提供内存降级策略；`AuthControllerTest` 增加刷新与注销用例。
- **审计日志**：`AuditTrailService` + `AuditEventMapper` 写入 `audit_events`，覆盖注册/登录/刷新/登出与健康档案操作；`V3__create_audit_events_table.sql` 完成表结构。
- **秘钥管理**：`JwtSecretResolver` 集成 `SecretManagerClient`，默认使用 `LocalSecretManagerClient` 从环境变量加载秘钥并校验位数。
- **测试基座**：`AbstractIntegrationTest` 引入 Testcontainers Redis，确保刷新令牌流程在测试环境可用。

## 待完成事项（Sprint 2）
- **[进行中]** 完成审计日志查询 API 及运维报表。
- **[待规划]** 与运维确认生产级 Secret Manager 对接与轮换策略。
- **[待规划]** 实现 Refresh Token 黑名单批量失效工具与运维脚本。

## 运行前置
1. 安装 JDK 17+、Maven 3.9+。
2. 安装 Docker 与 Docker Compose。
3. 克隆仓库并切换到 `HealthAi/` 目录。

```bash
mvn clean verify
```

## 本地环境启动
1. 配置环境变量：复制`.env.example`为`.env`，按需调整数据库账号密码。
2. 启动基础设施：
   ```bash
   docker-compose up -d
   ```
   - 会自动加载 `docs/database/schema.sql` 初始化数据库。
2. 启动 Spring Boot 应用：
   ```bash
   ./mvnw spring-boot:run
   ```
3. 访问接口：
   - API Swagger UI: [http://localhost:8081/swagger-ui](http://localhost:8081/swagger-ui)
   - Actuator Health: [http://localhost:9081/actuator/health](http://localhost:9081/actuator/health)

## 模块结构
```
src/main/java/com/example/healthai/
├── auth
├── consult
├── drug
├── notification
├── prescription
├── profile
├── vital
├── common (通用封装：API 响应、异常处理)
└── config (MyBatis、OpenAPI 等配置)
```

## 数据库
- DDL 文件：`docs/database/schema.sql`
- TODO：后续将引入 Flyway/Liquibase 处理自动迁移。

## 代码规范
- 提交信息遵循 `type(scope): summary`。
- 使用 `mvn clean verify` 进行本地检查。

## CI/CD
- GitHub Actions 工作流：`.github/workflows/ci.yml`
  - 触发条件：`main`、`develop` 分支的 push / PR
  - 步骤：Checkout → JDK 17 → `mvn clean verify`
  - 产物：Surefire 测试报告（作为 artifact 上传）

## Sprint 0 待办
- [x] 集成 Flyway 数据迁移
- [x] 完成 CI/CD 流水线（GitHub Actions）
- [ ] 编写 docker-compose 初始化脚本
- [ ] 添加更多模块实体与 Mapper

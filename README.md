# HealthAi Backend

## 概述
HealthAi 是一个基于 Spring Boot 的医疗健康后台系统，提供账号、健康档案、生理指标、问诊、药品库、处方与通知等模块。当前阶段进入 Sprint 3，聚焦问诊流程、提示词服务与 LLM 适配能力建设。

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Security / Spring Validation / Actuator
- MyBatis + MySQL
- Redis / Kafka（通过 Docker Compose 提供）
- SpringDoc OpenAPI 3

## 当前迭代进度（Sprint 3）
- **问诊服务规划**：`docs/sprint3-plan.md` 定义 `ConsultSvc`、`PromptSvc`、LLM 适配层的领域模型、接口与任务拆解。
- **PromptSvc 设计**：完成提示词执行服务 `PromptService`，支持模板变量合并与 LLM 调用。
- **LLM 适配方案**：落地 `OllamaLlmClient`、`HttpApiLlmClient` 与 `LlmConfiguration`，结合 `healthai.llm.*` 配置实现多渠道扩展与超时/重试策略。
- **Kafka 事件流**：新增 `healthai.consultations.*` 主题配置，通过 `ConsultationService` 发布创建、AI 复核、医生复核、关闭、失败等事件。
- **数据层建设**：新增 `V4~V6` Flyway 迁移与 `ConsultationMapper`、`PromptTemplateMapper` 等持久化组件，完成问诊与提示词的基础数据结构。

## 待完成事项（Sprint 3）
- **[进行中]** 实现 `ConsultSvc` REST API、状态机及数据持久化。
- **[待规划]** 完成 Prompt 模板管理接口与多渠道配置。
- **[待规划]** 集成 Kafka 事件发布与 `NotificationSvc` 通知触达。

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

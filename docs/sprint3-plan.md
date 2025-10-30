# Sprint 3 设计草案：ConsultSvc、PromptSvc 与 LLM 适配

## 1. 迭代目标
- 建立端到端的 AI 问诊能力，打通患者发起、AI 初诊、医生复核全流程。
- 提供可配置的提示词管理与多模型适配层，支持本地 `Ollama` 与外部大模型。
- 完善问诊过程的审计追踪、事件流与通知触达，为后续处方模块奠定数据基础。

## 2. 范围与交付物
- **ConsultSvc**：问诊域模型、状态机、REST API、与审计/通知的集成。
- **PromptSvc**：提示词模板管理、上下文拼装策略、面向 `ConsultSvc` 的服务接口。（已交付后台管理 API 与 `PromptTemplateAdminController`/`PromptTemplateAdminService`，支持模板增删改查、启停与版本管理）
- **LLM Adapter**：统一调用抽象，封装 `Ollama` 与 HTTP API 渠道，提供熔断与重试策略。
- **数据层扩展**：新增问诊、提示词、对话消息等表结构的 Flyway 脚本与 Mapper。
- **事件与通知**：Kafka 事件流（`healthai.consultations.*`）与告警/通知触发骨架。（`ConsultationService` 发布事件，`ConsultationEventListener` + `NotificationService` 消费；默认 `healthai.kafka.consultation.consumer-enabled=false`，需在部署环境开启并配置告警渠道）
- **测试与文档**：集成测试覆盖主要流程，完善 API 示例与系统文档。

## 3. 关键业务流程
- **问诊发起**：患者提交症状 → `ConsultSvc` 持久化 `draft` → 发布 `consultation.created` 事件。
- **AI 初诊**：`ConsultSvc` 触发 `PromptSvc` 生成上下文 → 调用 `LLM Adapter` 获取诊断 → 更新为 `ai_reviewed` 并记录对话历史。
- **医生复核**：医生（或模拟流程）提交复核意见 → 状态流转至 `doctor_reviewed`/`closed` → 记录审计事件 → 发布 `consultation.reviewed`。
- **通知触达**：当问诊状态变化或 AI 诊断失败时，向 `NotificationSvc` 发送告警/提醒消息。

## 4. 技术设计要点
### 4.1 ConsultSvc
- 领域实体：`Consultation`, `ConsultationMessage`, `ConsultationStatusTransition`。
- 状态机：`draft` → `ai_reviewed` → `doctor_reviewed` → `closed`（支持 `rejected`、`cancelled` 分支）。
- 接口：
  - `POST /api/v1/consultations` 创建问诊。
  - `GET /api/v1/consultations` 分页查询（状态/日期过滤）。
  - `GET /api/v1/consultations/{id}` 查看详情（含消息记录）。
  - `POST /api/v1/consultations/{id}/review` 医生复核。
  - `POST /api/v1/consultations/{id}/close` 手动结束/取消。
- 集成：
  - 使用 `AuditTrailService` 记录关键事件。
  - 发布 Kafka 事件供 `NotificationSvc` 与后续 `RxSvc` 订阅。
  - 通过 `PromptSvc` 完成 AI 调用，支持同步/异步执行模式。

### 4.2 PromptSvc
- 管理对象：`PromptTemplate`, `PromptVariable`, `PromptContextAssembler`。
- 提示词模板：存储在数据库，可通过管理接口（本迭代可提供内部配置）维护。
- 上下文拼装：组合用户档案、最新生理指标、历史问诊摘要形成提示词上下文。
- 接口：`generateConsultationPrompt(ConsultationContext)` 返回完整提示词；支持多语言与模型能力标记。

### 4.3 LLM Adapter
- 统一接口：`LLMClient#generate(LLMRequest)`。
- 渠道实现：`OllamaClient`, `HttpLLMClient`（配置外部供应商 URL、Key、超时）。
- 增强：熔断与重试（Resilience4j），超时控制，日志脱敏，错误分类（超时、重试、降级）。
- 配置：`application-*.yml` 定义渠道、模型、超时、并发限制；支持动态切换主备模型。

## 5. 数据模型与迁移
- **Flyway**：新增 `V4__create_consultations.sql`, `V5__create_consultation_messages.sql`, `V6__create_prompt_templates.sql`（编号待确认现有迁移数量）。
- 表设计重点：
  - `consultations`：状态、AI 诊断结果、医生意见、关联用户/医生、时间戳。
  - `consultation_messages`：对话历史、角色类型（patient/ai/doctor/system）、序号、内容摘要。
  - `prompt_templates`：模板名称、渠道、语言、版本、内容、变量定义。
- 索引：按用户/状态、创建时间排序；对话表按 `consultation_id`、`sequence`。

## 6. 事件与集成
- Kafka 主题：
  - `healthai.consultations.created`
  - `healthai.consultations.ai_reviewed`
  - `healthai.consultations.reviewed`
  - `healthai.consultations.closed`
- 事件载荷：`consultationId`, `patientId`, `status`, `timestamp`, `aiSummary`, `doctorSummary`。
- 通知策略：
  - `NotificationSvc` 订阅问诊状态事件，向患者推送状态更新。
  - 异常监控：AI 调用失败时发布 `healthai.consultations.failed`。

## 7. 安全与审计
- 认证：延续 Sprint 2 的 Access/Refresh Token 机制，对问诊接口启用 `patient`/`doctor` 角色鉴权。
- 审计：记录问诊创建、AI 调用结果、医生复核、手动关闭等事件；补充错误码（如 `CONSULTATION_NOT_FOUND`、`LLM_CALL_FAILED`）。
- 隐私：对提示词中敏感字段脱敏/最小化，限制日志输出。

## 8. 测试与质量保障
- 单元测试：
  - 状态机流转、Prompt 生成、LLM Adapter 错误处理。
- 集成测试：
  - Mock LLM 响应验证问诊流程。
  - Kafka 事件发布校验（可借助嵌入式 Kafka 或 Topic Mock）。
- 合规：
  - 定义伪造数据集用于本地测试，避免真实敏感信息。
  - 验证审计日志与安全策略生效。

## 9. 任务拆解与负责人建议
- **Domain & Persistence**：实体、Mapper、Flyway 脚本（负责人 A）。
- **ConsultSvc API 实现**：Service、Controller、DTO、状态机（负责人 B）。
- **PromptSvc & LLM Adapter**：提示词装配、模型调用、配置管理（负责人 C）。
- **事件与通知集成**：Kafka Producer、通知触发钩子（负责人 D，与 Notification 团队协作）。
- **测试与文档**：集成测试、API 文档、运维手册更新（负责人 E）。

## 10. 时间规划（建议 2 周）
- **第 1 周**：需求澄清、数据模型定稿、基础实体/迁移、PromptSvc/LLM Adapter 骨架。
- **第 2 周**：ConsultSvc 主干、事件/通知集成、测试覆盖、文档交付、预留 1~2 天缓冲。

## 11. 风险与缓解
- **模型响应不稳定**：准备默认兜底提示，支持重试与人工介入；必要时回退至静态建议。
- **外部依赖（Kafka/Notification）**：若尚未可用，需提供内存替代或 Mock，保证核心流程可测试。
- **性能与延迟**：AI 调用耗时长，需异步化与轮询机制；评估前端交互预期。
- **安全合规**：敏感数据泄露风险，需审查 Prompt 拼装与日志；遵守隐私要求。

## 12. 先决条件
- 确认 Sprint 2 审计与秘钥管理改动已合入主干。
- Kafka/Redis 环境可用，并配置好主题与权限。
- 明确医生角色账号或模拟方案，用于复核流程测试。

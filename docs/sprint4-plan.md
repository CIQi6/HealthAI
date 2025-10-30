# Sprint 4 计划：DrugSvc 与 RxSvc

## 1. 迭代目标
- 交付药品库维护、禁忌症校验与处方生成全流程。
- 建立问诊输出→处方服务的业务契约，确保医生复核后可生成处方。
- 补齐问诊相关审计查询、告警渠道对接等遗留项，为上线做好准备。

## 2. 范围与交付物
- **DrugSvc**：药品域模型、Flyway 脚本、Mapper、服务、控制器，支持 CRUD、分页搜索、禁忌症字段管理。
- **禁忌症校验**：在处方生成前结合患者档案/问诊信息执行禁忌症与剂量校验，提供错误码与提示信息。
- **RxSvc**：处方实体、处方/处方明细表、服务、控制器，完成处方创建、查询与明细展示。
- **问诊集成**：`ConsultationService` 输出补充处方所需字段，定义 AI/医生结论传递契约，必要时新增 Kafka 事件或 REST 接口。
- **审计与告警遗留项**：实现审计查询 API 初版；`AlertingService` 对接实际告警渠道（邮件或 Webhook）。
- **文档与运维**：更新 API 文档、数据库 ER、配置说明、运维 Runbook（含 Kafka、告警、处方发布流程）。

## 3. 里程碑
- **第 1-2 天**：数据模型审阅、Flyway 脚本、基础实体与 Mapper。
- **第 3-4 天**：DrugSvc & RxSvc 服务/控制器实现、禁忌症校验逻辑。
- **第 5 天**：与 ConsultSvc 对接、完成处方生成流程联调。
- **第 6 天**：审计查询 API、告警渠道接入、API/运维文档更新。
- **第 7 天**：集成测试与回归、性能基线、准备 Sprint Review Demo。

## 4. 关键任务拆解
- **Domain & Persistence（Owner A）**
  - `db/migration/V7__create_drugs.sql`：药品表（含禁忌症、剂量信息、版本、标签）。
  - `db/migration/V8__create_prescriptions.sql`、`V9__create_prescription_items.sql`：处方与明细表，外键指向问诊记录与药品。
  - Java 实体 + Mapper XML（`DrugMapper`, `PrescriptionMapper`, `PrescriptionItemMapper`）。

- **DrugSvc API（Owner B）**
  - `DrugService`, `DrugController` 实现列表/详情/新增/更新/删除。
  - 查询支持分页、关键字（通用名/标签）、按禁忌症过滤。
  - 对外 DTO、请求/响应校验。

- **禁忌症校验（Owner B）**
  - 定义 `ContraindicationEvaluator` 策略，结合患者档案、过敏史、慢性病信息。
  - 处方创建前校验，失败抛 `ErrorCode.DRUG_CONTRAINDICATED`，审计日志记录。
  - 单元测试覆盖常见禁忌场景。

- **RxSvc 处方流程（Owner C）**
  - `PrescriptionService`：创建处方、查询详情/列表、更新状态（草稿/已签发）。
  - `PrescriptionController`：REST API `/api/v1/prescriptions`。
  - 处方明细与药品绑定，支持多药组合与剂量校验。
  - 集成测试模拟问诊→医生复核→处方生成链路。

- **问诊集成（Owner C & ConsultSvc 团队）**
  - 在 `ConsultationService` 增补处方所需字段（医生复核意见、AI 建议、患者档案摘要）。
  - 定义处方触发契约（REST 调用或 Kafka 事件 `healthai.prescriptions.requested`）。
  - 若选用事件驱动，新增 `ConsultationEventPayload` 字段、`PrescriptionRequestListener`。

- **审计查询 & 告警接入（Owner E）**
  - 实现 `AuditEventAdminController` 查询接口（分页、条件过滤）。
  - `AlertingService` 接入实际渠道（SMTP、Webhook 或企业微信），提供配置项与失败重试策略。

- **测试与质量（Owner E）**
  - 单元测试覆盖药品禁忌、处方服务、审计查询。
  - 集成测试：
    - 问诊→处方 happy path。
    - 禁忌药品阻断路径。
    - 审计查询 API 权限校验。
  - 性能验证：药品搜索、处方生成在热点场景下延迟 < 500ms。

- **文档与运维（Owner E）**
  - 更新 `docs/database/` ER 图、字段说明。
  - 新增 `docs/runbooks/prescription-issuance.md` 说明运维流程。
  - 更新 README/Sprint4 计划链接。

## 5. 依赖与前置条件
- Sprint3 分支已合并主干，问诊事件与 Prompt 模板管理可用。
- Kafka 主题：`healthai.prescriptions.*`（若新增），需与运维确认创建。
- 邮件/Webhook 告警渠道凭据。
- 测试环境需准备药品种子数据与患者档案样例。

## 6. API 草案
- **药品**
  - `GET /api/v1/drugs`：参数 `keyword?`, `contraindication?`, `page`, `size`。
  - `POST /api/v1/drugs`：请求 `{genericName, indications, contraindications, dosage, tags}`。

- **处方**
  - `POST /api/v1/prescriptions`：`{consultationId, doctorId, items:[{drugId, dosageInstruction, days}], notes}`。
  - `GET /api/v1/prescriptions`：支持按 `consultationId`, `patientId`, `status` 过滤，分页。
  - `GET /api/v1/prescriptions/{id}`：返回处方详情、明细、禁忌校验结果。
- 新增 API 文档被前端验证可用。
- 审计与告警遗留项完成度 ≥ 80%，阻断上线风险清单清零。

## 7. 实施现状（2025-10-30）
- **DrugSvc 交付**：`DrugService` 与 `DrugController` 完成 CRUD、分页、禁忌过滤，集成 Flyway `V7__create_medicines_table.sql`。
- **RxSvc 交付**：`PrescriptionService`、`PrescriptionController` 支持创建/查询/状态流转，集成 `ContraindicationEvaluator`、审计、告警。
- **禁忌规则**：`SimpleContraindicationEvaluator` 覆盖过敏、慢性病、剂量、相互作用、特殊人群；审计写入 `contraindication_audit`。
- **测试覆盖**：
  - 单元：`SimpleContraindicationEvaluatorTest`、`PrescriptionServiceTest`。
  - 集成：`PrescriptionControllerIntegrationTest` 验证问诊→处方 happy/fail path。
- **待办**：告警渠道外部接入、运维 Runbook、Kafka 监控和审计查询 UI 优化。

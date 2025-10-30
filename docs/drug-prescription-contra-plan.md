# 药品/处方 ER 草稿与禁忌校验方案

## 1. 背景与目标
- **目标**：在现有 `DrugSvc`、`RxSvc` 规划基础上，完善药品与处方领域模型，并落地禁忌校验流程，确保处方签发安全合规。
- **依据**：参考 `docs/project.md` 的总体架构与数据模型，以及 `docs/sprint4-plan.md` 的迭代范围，补充药品/处方模块细化设计。

## 2. ER 草稿设计
- **`medicines` 表**
  - 主键：`id`（BIGINT，自增）。
  - 核心字段：`generic_name`（唯一）、`brand_name`、`indications`、`contraindications`（JSON/JSONB，含禁忌分类）、`dosage_guideline`、`drug_interactions`、`tags`、`created_at`、`updated_at`。
  - 约束：`generic_name` 唯一索引；禁忌字段按 `allergy`、`disease`、`age`、`pregnancy`、`renal`、`hepatic` 分类。
- **`medicine_attributes` 表（可选扩展）**
  - 字段：`id`、`medicine_id`、`attr_type`（allergy/disease/...）、`attr_code`、`severity`、`notes`。
  - 功能：结构化存储禁忌要素，便于高性能检索或同步外部药典。
- **`prescriptions` 表**
  - 字段：`id`、`consultation_id`、`patient_id`（快照）、`doctor_id`、`status`（draft/issued/cancelled）、`notes`、`contra_check_status`（pass/warn/fail）、`contra_fail_reason`、`created_at`、`updated_at`。
  - 说明：保留处方与患者的关键关联及禁忌校验结果。
- **`prescription_items` 表**
  - 字段：`id`、`prescription_id`、`medicine_id`、`dosage_instruction`、`frequency`、`day_supply`、`quantity`、`contra_result`、`created_at`。
  - 联合唯一约束：`prescription_id + medicine_id + dosage_instruction + frequency`。
- **`contraindication_audit` 表**
  - 字段：`id`、`prescription_id`、`prescription_item_id`、`check_time`、`checker`（system/manual）、`patient_snapshot`（JSON）、`violations`（JSON array）、`result`（pass/warn/fail）。
  - 用途：审计禁忌校验过程，支撑运维追踪与风控分析。
- **ER 关系汇总**
  - `consultations` 1 - n `prescriptions`
  - `prescriptions` 1 - n `prescription_items`
  - `prescription_items` 多对一 `medicines`
  - `medicines` 1 - n `medicine_attributes`
  - `prescriptions` 1 - n `contraindication_audit`

## 3. 禁忌校验流程
- **触发时机**
  - 处方草稿提交、医生签发、草稿修改均需重新执行；校验失败阻断状态流转。
- **输入数据**
  - `PrescriptionDraft`：处方项、剂量、频次。
  - `PatientProfileSnapshot`：慢性病、过敏史、遗传风险等（来自 `health_profiles`）。
  - `ConsultationSummary`：AI/医生诊断、关键症状、实验室数据（来自 `consultations`）。
  - 药品数据：`medicines.contraindications`、`medicine_attributes`、`drug_interactions`。
- **服务分层**
  - `ContraindicationEvaluator`
    - 策略组件：`AllergyRule`、`DiseaseRule`、`DrugInteractionRule`、`DemographicRule`（年龄/孕期）、`DoseLimitRule`。
    - 输出：`ContraResult`（pass/warn/fail + message + severity）。
  - `ContraindicationService`
    - 聚合策略生成 `ContraReport`；写入 `contraindication_audit`；更新 `prescriptions.contra_check_status`。
- **规则说明**
  - **过敏**：比对患者过敏史与药品禁忌 `allergy` 列表（建议使用标准化编码，如 ATC/SNOMED，短期可用内部枚举）。
  - **慢性病/疾病**：检查患者 `chronic_diseases` 对药品 `disease` 禁忌。
  - **药物相互作用**：对同一处方内药品组合查询 `drug_interactions`，阻断高危组合。
  - **剂量限制**：依据 `dosage_guideline` 中年龄/体重阈值校验 `dosage_instruction`。
  - **特殊人群**：孕妇、肝肾功能不全等通过档案扩展字段判断。
- **结果处理**
  - `fail`：返回错误码（如 `DRUG_CONTRAINDICATED_ALLERGY`），阻止处方签发。
  - `warn`：允许签发但需医生确认，标记 `contra_check_status=warn`。
  - 未知匹配：记录 `warn`，提示补全药品禁忌数据。
- **可观测性**
  - 指标：`contra_check_total`、`contra_check_fail`、`contra_check_warn`。
  - 日志与告警：`contraindication_audit` 记录详情，结合 `AlertingService` 推送高危失败至邮件/Webhook；Prometheus 暴露指标。
- **扩展方向**
  - 对接外部权威药典 API 同步禁忌数据。
  - 基于审计数据训练处方风险模型，支持智能预警。

## 4. 迭代规划拆解
- **任务 1：药品数据模型与迁移（Story Point 3）**
  - 验收：完成 `medicines`、`medicine_attributes`（如启用） Flyway 脚本与实体/Mapper；CRUD 与索引可用。
  - 依赖：无，优先实施。
- **任务 2：处方核心表与实体（SP 3）**
  - 验收：`prescriptions`、`prescription_items`、`contraindication_audit` 脚本及实体可用；外键/约束齐全。
  - 依赖：任务 1（药品表）。
- **任务 3：DrugSvc API 实现（SP 5）**
  - 验收：`DrugController` CRUD/分页/禁忌过滤；DTO 校验；单元与集成测试覆盖。
  - 依赖：任务 1。
- **任务 4：ContraindicationEvaluator 策略引擎（SP 5）**
  - 验收：策略组件输出 `ContraReport`；覆盖过敏、慢性病、相互作用、特殊人群、剂量；写入审计表；失败阻断处方。
  - 依赖：任务 1、任务 2。
- **任务 5：PrescriptionService & Controller（SP 8）**
  - 验收：处方创建/查询/签发 API；集成禁忌校验；问诊→处方集成测试涵盖成功与失败路径；审计记录可查。
  - 依赖：任务 2、任务 4，以及现有 `ConsultationService`。
- **任务 6：审计查询与告警接入（SP 3）**
  - 验收：`AuditEventAdminController` 支持分页筛选；`AlertingService` 推送禁忌失败；指标采集完成。
  - 依赖：任务 4、任务 5。
- **任务 7：文档与运维更新（SP 2）**
  - 验收：更新 `docs/database/` ER、`docs/runbooks/prescription-issuance.md`、API 文档；禁忌规则维护说明。
  - 依赖：任务 3-6。

## 5. 依赖顺序
1. 任务 1 → 任务 2：建立基础数据结构。
2. 任务 3 基于任务 1，可与任务 2 并行开发。
3. 任务 4 需依赖药品与处方结构（任务 1、2）。
4. 任务 5 依赖任务 2 与任务 4。
5. 任务 6 依赖任务 4、5 的审计/告警事件。
6. 任务 7 收尾整理，依赖核心功能完成。

## 6. 验收标准汇总
- **药品数据层**：Flyway 脚本执行成功，CRUD 测试通过。
- **处方流程**：禁忌校验阻断/警告逻辑符合设计，`contraindication_audit` 可追溯。
- **集成链路**：问诊→处方→审计→告警集成测试通过。
- **文档输出**：ER 图、API 文档、处方运维 Runbook 更新并可供评审。

---

> 本文档用于指导 Sprint 4 药品与处方模块实现，后续如有数据模型或流程调整，请同步更新并提交评审。

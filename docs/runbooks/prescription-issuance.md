# 处方发布运维 Runbook

## 目的
指导运维人员在开发、测试与生产环境中部署、验证、监控 `PrescriptionService` 及相关依赖，确保处方生成、禁忌校验与审计链路稳定运行。

## 组件与依赖
- **核心服务**：`healthai` 应用中的 `PrescriptionService`、`DrugService`、`ConsultationService`、`SimpleContraindicationEvaluator`。
- **数据库**：MySQL `healthai` 库，表 `prescriptions`、`prescription_items`、`drugs`、`contraindication_audit`。
- **消息系统**：Kafka（主题 `healthai.consultations.*`），用于问诊状态变更与处方触发事件。
- **缓存/会话**：Redis 用于会话缓存、幂等去重、短期状态。
- **告警/监控**：Prometheus、Grafana、ELK；外部告警渠道由其他团队维护。

## 部署前检查
- **配置项**
  - 通过环境变量覆盖 `SPRING_DATASOURCE_*`、`KAFKA_BOOTSTRAP_SERVERS`、`HEALTHAI_SECURITY_JWT_SECRET`、`HEALTHAI_LLM_*` 等关键配置。
  - 确认 `healthai.kafka.consultation.consumer-enabled` 按环境设置正确（生产应为 `true`）。
- **数据库迁移**
  - 执行 `./mvnw -Pprod flyway:migrate` 或在 CI/CD 管道运行 Flyway，确保 `V7__create_medicines_table.sql`、`V8__create_prescriptions.sql`、`V9__create_prescription_items.sql`、`V10__create_contraindication_audit.sql` 等脚本顺利执行。
  - 若执行失败需先回滚，确认 schema 与数据一致性。
- **基础设施健康**
  - MySQL、Redis、Kafka 均需处于可用状态，具备可用的监控与备份策略。
- **密钥与凭据**
  - 检查 JWT 密钥、Kafka 认证（如启用）、LLM API Key（如启用外部模型）。

## 发布步骤
1. 获取目标版本镜像或执行 `./mvnw clean package -DskipTests` 构建后推送镜像。
2. 按“数据库 → Redis → Kafka → 应用”顺序确保依赖服务已启动并正常。
3. 在目标环境部署 `healthai` 应用（Docker Compose 或 K8s），加载正确配置。
4. 验证健康状态：
   - `GET /actuator/health`（期望状态 `UP`）。
   - `GET /api/v1/prescriptions/probe`（返回版本号/环境信息）。

## 功能验证
- **Happy Path**：调用 `POST /api/v1/prescriptions` 创建处方，确认返回 `201 Created` 且输出包含正确明细和禁忌校验结果。
- **禁忌阻断**：构造含禁忌药品的请求，期望返回 `ErrorCode.DRUG_CONTRAINDICATED`，并在 `contraindication_audit` 记录拒绝原因。
- **审计日志**：`GET /api/v1/admin/audit-events?resourceType=RX_PRESCRIPTION`，确认处方创建操作被记录。
- **查询能力**：验证 `GET /api/v1/prescriptions`（分页过滤）和 `GET /api/v1/prescriptions/{id}` 正常返回。

## 监控与指标
- **Actuator / Prometheus**
  - `http_server_requests_seconds{uri="/api/v1/prescriptions"}`：99 分位需 < 500 ms。
  - `jdbc_connections_active`、`hikaricp_connections_active`：保持在连接池上限 80% 以下。
  - 如启用自定义指标 `prescription_contraindication_fail_total`，监控禁忌拒绝数量趋势。
- **日志**
  - 聚焦 `com.example.healthai.prescription`、`com.example.healthai.contra` 包的 INFO/ERROR。关键日志需在 ELK 建立 Saved Search。
- **Kafka Lag**
  - 使用 `kafka-consumer-groups --describe` 关注 `healthai-<env>` 消费组滞后，滞后超阈值触发告警。
- **数据库**
  - 监控 MySQL 慢查询日志与锁等待。必要时通过 `performance_schema` 分析。

## 常见故障与排查
- **创建处方返回 4XX**
  - 检查请求体是否缺失 `consultationId`、`items` 等字段。
  - 确认患者档案数据完整，查看 `contraindication_audit`。
- **返回 5XX**
  - 查看应用日志和 Actuator `loggers` 端点。重点检查数据库连接耗尽、Kafka 超时、LLM 请求失败。
- **Kafka 事件未触发**
  - 确认 `healthai.kafka.consultation.consumer-enabled=true`。
  - 查看消费日志中的反序列化错误或权限问题。
- **禁忌规则误判**
  - 开启 DEBUG 日志重放请求，检查 `SimpleContraindicationEvaluator` 规则命中情况。
  - 核对 `drugs` 数据与患者档案是否匹配。
- **数据库连接耗尽**
  - 查看 `hikaricp.connections.active` 指标。
  - 优化慢查询或调大连接池配置（`application.yml` 中的 `hikari.maximum-pool-size`）。

## 回滚策略
- 保留上一版本镜像，必要时直接回滚部署。
- 迁移不可逆时提前执行 `mysqldump` 备份；如可用 Flyway Undo，执行 `flyway:undo` 前务必确认影响。
- 回滚后重新执行健康检查与功能验证，确保处方数据未损坏。

## 参考资料与责任人
- 接口文档：`docs/api/prescriptions.md`、`docs/api/drugs.md`。
- 禁忌规则：`docs/drug-prescription-contra-plan.md`。
- 联系人：
  - 后端负责人（Owner B）。
  - 禁忌策略与医学审核（Owner C）。
  - 监控与告警平台团队。


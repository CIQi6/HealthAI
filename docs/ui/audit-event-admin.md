# Audit Event Admin UI 指南

## 功能目标
- 为管理员提供审计事件检索界面，支持按资源类型、动作、操作者、时间范围筛选。
- 结合后端 `AuditEventAdminController`（`/api/v1/admin/audit-events`）实现分页展示与导出。
- 优化问题排查体验，缩短安全审计响应时间。

## 交互设计
- **过滤区**：
  - `Resource Type`（下拉，多选：`CONSULTATION`、`RX_PRESCRIPTION`、`AUTH` 等）。
  - `Action`（自动补全，基于 `/metadata/audit-actions` 接口返回枚举，若未实现则使用本地常量配置）。
  - `Actor ID`（数字输入，可选）。
  - `时间范围`（日期时间选择器，默认最近 24 小时）。
- **结果表格**：列 `OccurredAt`、`Actor`、`Action`、`ResourceType`、`ResourceId`、`SourceIP`、`Message/Metadata`。
- **分页控件**：每页 20 条，支持自定义 20/50/100；滚动时固定过滤区。
- **导出按钮**：调用后端导出接口（待规划）；临时方案可直接复制 JSON。
- **详情抽屉**：点击行展开，展示 `metadata` JSON 与 `createdAt/updatedAt`。

## API 对接
- 请求：`GET /api/v1/admin/audit-events`
  - 参数：`resourceType?`、`action?`、`actorId?`、`from?`、`to?`、`page=0`、`size=20`
  - Header：`Authorization: Bearer <ADMIN_TOKEN>`
- 响应：`ApiResponse<PageResponse<AuditEventResponse>>`
  - `data.content` 为列表，字段参考 `AuditEventResponse`。
  - `data.totalElements`、`data.page`、`data.size` 用于分页。
- 错误处理：
  - `401/403`：未授权或权限不足，前端跳转登录或展示提示。
  - `422`：参数校验失败，展示具体错误信息。

## 性能与稳定性检查
- 后端分页限制 `size ≤ 200`，前端保持默认 20，导出场景使用后台任务。
- 界面加载时显示 Skeleton，防止大列表渲染卡顿。
- 若调用失败，展示重试按钮并记录到告警系统。
- 与 `AuditEventAdminControllerTest` 保持一致的过滤逻辑：
  - 发送 ISO DATETIME 格式（例如 `2025-10-30T08:30:00`）。

## 后续增强
- 增加导出 CSV/Excel 的后端接口并在 UI 中调用。
- 支持对 `metadata` JSON 进行可视化展示（如树形组件）。
- 与告警面板联动：点击告警跳转到对应 audit event。


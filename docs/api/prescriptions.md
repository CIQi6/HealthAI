# Prescription API 契约

## 基础信息
- **Base Path**: `/api/v1/prescriptions`
- **认证**: 需携带 `Authorization: Bearer <token>`，医生角色方可创建/签发，患者可查询自身。
- **统一响应**: `ApiResponse<T>`，成功时 `code=0`、`message="OK"`、`data` 为业务内容。

## 枚举说明
- `status`: `DRAFT` | `PENDING_REVIEW` | `ISSUED` | `CANCELLED`
- `contraStatus`: `PASS` | `WARN` | `FAIL`

## 创建处方
- **Method**: `POST`
- **Path**: `/api/v1/prescriptions`
- **描述**: 医生在问诊复核后创建处方。
- **请求体** (`application/json`):
```json
{
  "consultationId": 1001,
  "doctorId": 3001,
  "notes": "多喝水，按时服药",
  "items": [
    {
      "drugId": 501,
      "dosageInstruction": "500mg",
      "frequency": "BID",
      "daySupply": 7,
      "quantity": 14
    }
  ]
}
```
- **字段约束**:
  - `consultationId`、`doctorId` 必填，需存在对应问诊与医生。
  - `items` 至少包含 1 个药品。
  - `dosageInstruction`、`frequency`、`daySupply`、`quantity` 依据药品规则校验。
- **响应** `201 Created`:
```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 9001,
    "consultationId": 1001,
    "patientId": 2002,
    "doctorId": 3001,
    "status": "DRAFT",
    "contraStatus": "PASS",
    "contraFailReason": null,
    "notes": "多喝水，按时服药",
    "createdAt": "2025-10-30T08:21:33",
    "updatedAt": "2025-10-30T08:21:33",
    "items": [
      {
        "id": 9101,
        "medicineId": 501,
        "genericName": "Azithromycin",
        "brandName": "Pulixin",
        "dosageInstruction": "500mg",
        "frequency": "BID",
        "daySupply": 7,
        "quantity": 14,
        "contraResult": "PASS",
        "contraindications": {
          "allergy": ["macrolides"],
          "warnings": ["liver dysfunction"]
        }
      }
    ],
    "audits": []
  }
}
```
- **错误码**:
  - `CONSULTATION_NOT_FOUND`: 问诊不存在。
  - `DRUG_NOT_FOUND`: 药品不存在。
  - `DRUG_CONTRAINDICATED`: 禁忌校验失败，`message` 包含原因。
  - `VALIDATION_FAILED`: 请求参数非法。

## 查询处方列表
- **Method**: `GET`
- **Path**: `/api/v1/prescriptions`
- **描述**: 支持按问诊、患者、医生、状态筛选并分页。
- **查询参数**:
  - `consultationId?`
  - `patientId?`
  - `doctorId?`
  - `status?`
  - `page` (默认 0)
  - `size` (默认 20，最大 100)
- **响应** `200 OK`:
```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "content": [
      {
        "id": 9001,
        "consultationId": 1001,
        "patientId": 2002,
        "doctorId": 3001,
        "status": "DRAFT",
        "contraStatus": "PASS",
        "createdAt": "2025-10-30T08:21:33",
        "updatedAt": "2025-10-30T08:21:33"
      }
    ],
    "totalElements": 1,
    "page": 0,
    "size": 20
  }
}
```

## 查询处方详情
- **Method**: `GET`
- **Path**: `/api/v1/prescriptions/{id}`
- **描述**: 获取处方详情、药品明细、禁忌审计记录。
- **响应** `200 OK`: 同创建处方响应里 `data` 结构。
- **错误码**: `PRESCRIPTION_NOT_FOUND`。

## 更新处方状态
- **Method**: `PUT`
- **Path**: `/api/v1/prescriptions/{id}/status`
- **描述**: 医生将处方签发或取消。
- **请求体**:
```json
{
  "status": "ISSUED"
}
```
- **业务规则**:
  - 仅 `DRAFT` / `PENDING_REVIEW` 状态可更新。
  - 当禁忌状态为 `FAIL` 时禁止签发，将返回 `DRUG_CONTRAINDICATED`。
- **响应** `200 OK`: 返回最新处方详情。

## 健康探针
- **Method**: `GET`
- **Path**: `/api/v1/prescriptions/probe`
- **描述**: 运维自检，返回字符串 `"prescription-service-ok"`。

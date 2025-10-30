# Drug API 契约

## 基础信息
- **Base Path**: `/api/v1/drugs`
- **认证**: 管理员角色可创建/更新/删除，医生/患者支持查询。
- **统一响应**: `ApiResponse<T>`。

## 创建药品
- **Method**: `POST`
- **Path**: `/api/v1/drugs`
- **请求体**:
```json
{
  "genericName": "Ibuprofen",
  "brandName": "芬必得",
  "indications": "解热、镇痛",
  "contraindications": {
    "allergy": ["NSAIDs"],
    "pregnancy": "慎用"
  },
  "dosageGuideline": {
    "adult": "200-400mg q6h",
    "maxDaily": "1200mg"
  },
  "drugInteractions": {
    "anticoagulants": "增加出血风险"
  },
  "tags": ["OTC", "pain"]
}
```
- **响应** `201 Created`: 返回 `MedicineResponse`。

## 更新药品
- **Method**: `PUT`
- **Path**: `/api/v1/drugs/{id}`
- **描述**: 更新药品信息，字段与创建相同。

## 查询药品列表
- **Method**: `GET`
- **Path**: `/api/v1/drugs`
- **查询参数**:
  - `keyword?`：匹配通用名/品牌名/标签。
  - `contraindication?`：按禁忌类型过滤。
  - `page`（默认 0）、`size`（默认 20，最大 50）。
- **响应** `200 OK`：`PageResponse<MedicineResponse>`。

## 查询药品详情
- **Method**: `GET`
- **Path**: `/api/v1/drugs/{id}`
- **描述**: 返回单个药品信息。

## 删除药品
- **Method**: `DELETE`
- **Path**: `/api/v1/drugs/{id}`
- **响应** `204 No Content`。

## 健康探针
- **Method**: `GET`
- **Path**: `/api/v1/drugs/probe`
- **描述**: 返回字符串 `"drug-service-ok"`。

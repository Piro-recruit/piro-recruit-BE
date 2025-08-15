# Google Form API 명세서

## 개요
구글 폼 관리를 위한 REST API 명세서입니다.

## Base URL
```
/api/google-forms
```

## 공통 응답 형식
```json
{
  "success": true,
  "data": {...},
  "message": "...",
  "status": 200,
  "code": 0,
  "time": "2025-01-15T10:30:00"
}
```

## 에러 코드
| 코드 | 메시지 |
|------|--------|
| 2201 | 해당 구글 폼을 찾을 수 없습니다. |
| 2202 | 현재 활성화된 구글 폼이 없습니다. |
| 2203 | 이미 등록된 구글폼 ID입니다. |
| 2204 | 현재 활성화된 구글 폼은 삭제할 수 없습니다. |

## API 엔드포인트

총 12개의 API 엔드포인트가 제공됩니다.

### 1. 구글 폼 생성
**POST** `/api/google-forms`

구글 폼을 새로 생성합니다.

#### Request Body
```json
{
  "formId": "1FAIpQLSe...",
  "title": "25기 리크루팅",
  "formUrl": "https://forms.gle/...",
  "sheetUrl": "https://docs.google.com/spreadsheets/...",
  "description": "25기 리크루팅 폼입니다.",
  "recruitingStartDate": "2025-01-15T09:00:00",
  "recruitingEndDate": "2025-01-30T23:59:59"
}
```

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "formId": "1FAIpQLSe...",
    "title": "25기 리크루팅",
    "formUrl": "https://forms.gle/...",
    "sheetUrl": "https://docs.google.com/spreadsheets/...",
    "isActive": false,
    "description": "25기 리크루팅 폼입니다.",
    "recruitingStartDate": "2025-01-15T09:00:00",
    "recruitingEndDate": "2025-01-30T23:59:59",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "message": "구글 폼이 성공적으로 생성되었습니다."
}
```

### 2. 전체 구글 폼 조회
**GET** `/api/google-forms`

모든 구글 폼을 최신순으로 조회합니다.

#### Query Parameters
- `includeApplicationCount` (boolean, optional): 지원서 개수 포함 여부 (기본값: true)

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "formId": "1FAIpQLSe...",
      "title": "25기 리크루팅",
      "formUrl": "https://forms.gle/...",
      "sheetUrl": "https://docs.google.com/spreadsheets/...",
      "isActive": true,
      "description": "25기 리크루팅 폼입니다.",
      "recruitingStartDate": "2025-01-15T09:00:00",
      "recruitingEndDate": "2025-01-30T23:59:59",
      "applicationCount": 15,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ],
  "message": "1개의 구글 폼을 조회했습니다."
}
```

### 3. 특정 구글 폼 조회 (ID)
**GET** `/api/google-forms/{id}`

ID를 기준으로 특정 구글 폼을 조회합니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Query Parameters
- `includeApplicationCount` (boolean, optional): 지원서 개수 포함 여부 (기본값: true)

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "formId": "1FAIpQLSe...",
    "title": "25기 리크루팅",
    "formUrl": "https://forms.gle/...",
    "sheetUrl": "https://docs.google.com/spreadsheets/...",
    "isActive": true,
    "description": "25기 리크루팅 폼입니다.",
    "recruitingStartDate": "2025-01-15T09:00:00",
    "recruitingEndDate": "2025-01-30T23:59:59",
    "applicationCount": 15,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

### 4. 폼 ID로 구글 폼 조회
**GET** `/api/google-forms/form-id/{formId}`

구글 폼 ID를 기준으로 구글 폼을 조회합니다.

#### Path Parameters
- `formId` (String): 구글 폼 ID

#### Query Parameters
- `includeApplicationCount` (boolean, optional): 지원서 개수 포함 여부 (기본값: true)

### 5. 현재 활성화된 구글 폼 조회
**GET** `/api/google-forms/active`

현재 활성화된 구글 폼을 조회합니다.

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "formId": "1FAIpQLSe...",
    "title": "25기 리크루팅",
    "formUrl": "https://forms.gle/...",
    "sheetUrl": "https://docs.google.com/spreadsheets/...",
    "isActive": true,
    "description": "25기 리크루팅 폼입니다.",
    "recruitingStartDate": "2025-01-15T09:00:00",
    "recruitingEndDate": "2025-01-30T23:59:59",
    "applicationCount": 15,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

### 6. 구글 폼 활성화
**PUT** `/api/google-forms/{id}/activate`

특정 구글 폼을 활성화합니다. (기존 활성화된 것은 비활성화)

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "formId": "1FAIpQLSe...",
    "title": "25기 리크루팅",
    "formUrl": "https://forms.gle/...",
    "sheetUrl": "https://docs.google.com/spreadsheets/...",
    "isActive": true,
    "description": "25기 리크루팅 폼입니다.",
    "recruitingStartDate": "2025-01-15T09:00:00",
    "recruitingEndDate": "2025-01-30T23:59:59",
    "applicationCount": 15,
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "message": "구글 폼이 활성화되었습니다."
}
```

### 7. 구글 폼 비활성화
**PUT** `/api/google-forms/{id}/deactivate`

특정 구글 폼을 비활성화합니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

### 8. 구글 폼 URL 업데이트
**PUT** `/api/google-forms/{id}/form-url`

구글 폼의 URL을 업데이트합니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Request Body
```json
{
  "formUrl": "https://forms.gle/new-url"
}
```

### 9. 구글 시트 URL 업데이트
**PUT** `/api/google-forms/{id}/sheet-url`

구글 시트의 URL을 업데이트합니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Request Body
```json
{
  "sheetUrl": "https://docs.google.com/spreadsheets/new-url"
}
```

### 10. 구글 폼 통계 정보 조회
**GET** `/api/google-forms/{id}/statistics`

특정 구글 폼의 상세 통계 정보를 조회합니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "googleFormId": 1,
    "formId": "1FAIpQLSe...",
    "formTitle": "25기 리크루팅",
    "totalApplications": 15,
    "statusStatistics": {
      "PENDING": 5,
      "COMPLETED": 8,
      "FAILED": 2
    },
    "isActive": true,
    "createdAt": "2025-01-15T10:30:00"
  },
  "message": "구글 폼 통계 정보를 조회했습니다."
}
```

### 11. 제목으로 구글 폼 검색
**GET** `/api/google-forms/search`

제목을 기준으로 구글 폼을 검색합니다.

#### Query Parameters
- `title` (String, required): 검색할 제목

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "formId": "1FAIpQLSe...",
      "title": "25기 리크루팅",
      "formUrl": "https://forms.gle/...",
      "sheetUrl": "https://docs.google.com/spreadsheets/...",
      "isActive": true,
      "description": "25기 리크루팅 폼입니다.",
      "recruitingStartDate": "2025-01-15T09:00:00",
      "recruitingEndDate": "2025-01-30T23:59:59",
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ],
  "message": "'25기'로 검색된 구글 폼 1개를 조회했습니다."
}
```

### 12. 구글 폼 삭제
**DELETE** `/api/google-forms/{id}`

특정 구글 폼을 삭제합니다. 활성화된 폼은 삭제할 수 없습니다.

#### Path Parameters
- `id` (Long): 구글 폼 ID

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "구글 폼이 성공적으로 삭제되었습니다."
}
```

#### Error Response (400 Bad Request)
```json
{
  "success": false,
  "data": null,
  "message": "현재 활성화된 구글 폼은 삭제할 수 없습니다.",
  "status": 400,
  "code": 2204
}
```

## 데이터 모델

### GoogleFormRequest
```json
{
  "formId": "string (required, max 100)",
  "title": "string (required, 2-100 characters)",
  "formUrl": "string (required, valid URL, max 1000)",
  "sheetUrl": "string (optional, valid URL, max 1000)",
  "description": "string (optional, max 2000)",
  "recruitingStartDate": "string (ISO 8601 DateTime)",
  "recruitingEndDate": "string (ISO 8601 DateTime)"
}
```

### GoogleFormResponse
```json
{
  "id": "number",
  "formId": "string",
  "title": "string",
  "formUrl": "string",
  "sheetUrl": "string",
  "isActive": "boolean",
  "description": "string",
  "recruitingStartDate": "string (ISO 8601 DateTime)",
  "recruitingEndDate": "string (ISO 8601 DateTime)",
  "applicationCount": "number (optional)",
  "createdAt": "string (ISO 8601 DateTime)",
  "updatedAt": "string (ISO 8601 DateTime)"
}
```

## 비즈니스 규칙

1. **폼 ID 유니크**: 동일한 `formId`로 여러 구글 폼을 생성할 수 없습니다.
2. **활성화 규칙**: 하나의 구글 폼만 활성화될 수 있으며, 새로운 폼을 활성화하면 기존 활성화된 폼은 자동으로 비활성화됩니다.
3. **삭제 제한**: 활성화된 구글 폼은 삭제할 수 없습니다. 먼저 비활성화 후 삭제해야 합니다.
4. **URL 유효성**: 모든 URL은 http:// 또는 https://로 시작해야 합니다.
5. **리쿠르팅 기간**: 시작 날짜와 종료 날짜를 설정할 수 있으며, 선택적 필드입니다.
# Webhook Application API 명세서

구글 폼 웹훅 지원서 관리 API

## 목차

- [개요](#개요)
- [인증](#인증)
- [API 엔드포인트](#api-엔드포인트)
  - [지원서 데이터 수신](#지원서-데이터-수신)
  - [지원서 조회](#지원서-조회)
  - [지원서 상태 관리](#지원서-상태-관리)
  - [통계 및 모니터링](#통계-및-모니터링)
- [데이터 모델](#데이터-모델)
- [오류 코드](#오류-코드)

## 개요

Webhook Application API는 구글 폼에서 전송된 지원서 데이터를 받아 저장하고 관리하는 시스템입니다. 실시간 지원서 수집, 상태 추적, 평가 관리 등의 기능을 제공합니다.

**Base URL:** `/api/webhook/applications`

## 인증

- **지원서 수신**: 웹훅 토큰 인증 필요
- **관리 기능**: Admin 권한 필요 (`@RequireAdmin`)
- **상태 변경**: Root 권한 필요 (`@RequireRoot`)

## API 엔드포인트

### 지원서 데이터 수신

#### POST /receive
구글 폼에서 전송된 지원서 데이터를 받아 저장합니다.

**요청 헤더**
```
Content-Type: application/json
X-Webhook-Token: {WEBHOOK_API_KEY}
```

**요청 본문**
```json
{
  "formId": "1FAIpQLSe...",
  "applicantName": "홍길동",
  "applicantEmail": "hong@example.com",
  "formResponseId": "2_ABaOnud...",
  "submissionTimestamp": "2024-01-01T10:00:00",
  "school": "서울대학교",
  "department": "컴퓨터공학과", 
  "grade": "3학년",
  "major": "컴퓨터공학",
  "phoneNumber": "010-1234-5678",
  "formData": {
    "question1": "답변 내용...",
    "question2": "답변 내용..."
  }
}
```

**응답 예시**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "googleFormId": 1,
    "formId": "1FAIpQLSe...",
    "formTitle": "25기 리크루팅",
    "applicantName": "홍길동",
    "applicantEmail": "hong@example.com",
    "status": "PENDING",
    "passStatus": "PENDING",
    "createdAt": "2024-01-01T10:00:00"
  },
  "message": "지원서가 성공적으로 저장되었습니다."
}
```

### 지원서 조회

#### GET /
전체 지원서 목록을 최신순으로 조회합니다. (Admin 권한 필요)

**응답 예시**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "applicantName": "홍길동",
      "applicantEmail": "hong@example.com",
      "averageScore": 82.5,
      "evaluationCount": 3,
      "passStatus": "PENDING",
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "message": "15개의 지원서를 조회했습니다."
}
```

#### GET /google-form/{googleFormId}
특정 구글 폼의 모든 지원서를 조회합니다.

**경로 매개변수**
- `googleFormId`: 구글 폼 ID (Long)

#### GET /form-id/{formId}
특정 폼 ID의 모든 지원서를 조회합니다.

**경로 매개변수**
- `formId`: 구글 폼 식별자 (String)

#### GET /id/{id}
ID를 기준으로 특정 지원서를 조회합니다.

**경로 매개변수**
- `id`: 지원서 ID (Long)

#### GET /by-email
이메일을 기준으로 지원서를 조회합니다.

**쿼리 매개변수**
- `email`: 지원자 이메일 (String)

#### GET /google-form/{googleFormId}/by-email
특정 구글 폼에서 이메일을 기준으로 지원서를 조회합니다.

**경로 매개변수**
- `googleFormId`: 구글 폼 ID (Long)

**쿼리 매개변수**
- `email`: 지원자 이메일 (String)

#### GET /form-id/{formId}/by-email
특정 폼 ID에서 이메일을 기준으로 지원서를 조회합니다.

**경로 매개변수**
- `formId`: 구글 폼 식별자 (String)

**쿼리 매개변수**
- `email`: 지원자 이메일 (String)

#### GET /by-status
처리 상태를 기준으로 지원서를 조회합니다.

**쿼리 매개변수**
- `status`: 처리 상태 (`PENDING`, `COMPLETED`, `FAILED`)

#### GET /google-form/{googleFormId}/by-status
특정 구글 폼에서 처리 상태를 기준으로 지원서를 조회합니다.

**경로 매개변수**
- `googleFormId`: 구글 폼 ID (Long)

**쿼리 매개변수**
- `status`: 처리 상태 (`PENDING`, `COMPLETED`, `FAILED`)

### 지원서 상태 관리

#### POST /{id}/status
특정 지원서의 합격 상태를 변경합니다. (Root 권한 필요)

**경로 매개변수**
- `id`: 지원서 ID (Long)

**쿼리 매개변수**
- `passStatus`: 변경할 합격 상태 (`PENDING`, `FIRST_PASS`, `FINAL_PASS`, `FAILED`)

**응답 예시**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "applicantName": "홍길동",
    "passStatus": "FIRST_PASS",
    "updatedAt": "2024-01-01T15:00:00"
  },
  "message": "지원서 상태가 FIRST_PASS로 변경되었습니다."
}
```

#### POST /bulk-status
점수 상위 N명의 지원서 상태를 일괄 변경합니다. (Root 권한 필요)

**쿼리 매개변수**
- `topN`: 변경할 상위 인원 수 (Integer)
- `passStatus`: 변경할 합격 상태 (`PENDING`, `FIRST_PASS`, `FINAL_PASS`, `FAILED`)

**응답 예시**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "applicantName": "홍길동",
      "passStatus": "FIRST_PASS"
    },
    {
      "id": 2,
      "applicantName": "김철수",
      "passStatus": "FIRST_PASS"
    }
  ],
  "message": "상위 2명의 지원서 상태가 FIRST_PASS로 변경되었습니다."
}
```

### 통계 및 모니터링

#### GET /check
이메일을 기준으로 지원서 제출 여부를 확인합니다.

**쿼리 매개변수**
- `email`: 확인할 이메일 (String)

**응답 예시**
```json
{
  "success": true,
  "data": {
    "email": "hong@example.com",
    "submitted": true,
    "status": "submitted"
  }
}
```

#### GET /google-form/{googleFormId}/check
특정 구글 폼에서 이메일을 기준으로 지원서 제출 여부를 확인합니다.

#### GET /form-id/{formId}/check
특정 폼 ID에서 이메일을 기준으로 지원서 제출 여부를 확인합니다.

#### GET /pending-count
처리 대기 중인 지원서의 개수를 조회합니다.

**응답 예시**
```json
{
  "success": true,
  "data": {
    "pendingCount": 23,
    "message": "처리 대기 중인 지원서 23개"
  }
}
```

#### GET /google-form/{googleFormId}/count
특정 구글 폼의 총 지원서 개수를 조회합니다.

#### GET /form-id/{formId}/count
특정 폼 ID의 총 지원서 개수를 조회합니다.

#### GET /statistics
전체 지원서의 상태별 통계를 조회합니다.

**응답 예시**
```json
{
  "success": true,
  "data": {
    "PENDING": 15,
    "COMPLETED": 8,
    "FAILED": 2
  },
  "message": "상태별 통계를 조회했습니다."
}
```

#### GET /google-form/{googleFormId}/statistics
특정 구글 폼의 상태별 통계를 조회합니다.

#### POST /test
Apps Script와의 연결을 테스트하기 위한 엔드포인트입니다.

**요청 본문**
```json
{
  "test": "data"
}
```

**응답 예시**
```json
{
  "success": true,
  "data": {
    "status": "success",
    "message": "웹훅 연결이 정상적으로 작동합니다.",
    "timestamp": 1704067200000,
    "receivedData": {
      "test": "data"
    }
  },
  "message": "웹훅 테스트 성공"
}
```

## 데이터 모델

### WebhookApplicationRequest
```json
{
  "formId": "String (required)",
  "applicantName": "String (required)",
  "applicantEmail": "String (required, email format)",
  "formResponseId": "String (required)",
  "submissionTimestamp": "LocalDateTime (required)",
  "school": "String (optional)",
  "department": "String (optional)",
  "grade": "String (optional)",
  "major": "String (optional)",
  "phoneNumber": "String (optional)",
  "formData": "Map<String, Object> (required)"
}
```

### WebhookApplicationResponse
```json
{
  "id": "Long",
  "googleFormId": "Long",
  "formId": "String",
  "formTitle": "String",
  "applicantName": "String",
  "applicantEmail": "String",
  "formResponseId": "String",
  "submissionTimestamp": "LocalDateTime",
  "school": "String",
  "department": "String",
  "grade": "String",
  "major": "String",
  "phoneNumber": "String",
  "formData": "Map<String, Object>",
  "status": "String (PENDING, COMPLETED, FAILED)",
  "errorMessage": "String",
  "aiAnalysis": "Map<String, Object>",
  "averageScore": "Double",
  "evaluationCount": "Integer",
  "passStatus": "String (PENDING, FAILED, FIRST_PASS, FINAL_PASS)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### ProcessingStatus
- `PENDING`: 처리 대기
- `COMPLETED`: 처리 완료
- `FAILED`: 처리 실패

### PassStatus
- `PENDING`: 평가 대기
- `FAILED`: 불합격
- `FIRST_PASS`: 1차 합격
- `FINAL_PASS`: 최종 합격

## 오류 코드

| 상태 코드 | 오류 유형 | 설명 |
|---------|---------|------|
| 400 | Bad Request | 잘못된 요청 형식 또는 유효성 검증 실패 |
| 401 | Unauthorized | 인증되지 않은 요청 |
| 403 | Forbidden | 권한이 없는 접근 |
| 404 | Not Found | 지원서를 찾을 수 없음 |
| 409 | Conflict | 중복된 지원서 |
| 500 | Internal Server Error | 서버 내부 오류 |

**오류 응답 예시**
```json
{
  "success": false,
  "data": null,
  "message": "지원서 ID 999를 찾을 수 없습니다.",
  "status": 404,
  "code": 4005,
  "time": "2024-01-01T10:00:00"
}
```
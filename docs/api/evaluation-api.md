# Evaluation API 명세서

지원서 평가 관리 API

## 목차

- [개요](#개요)
- [인증 및 권한](#인증-및-권한)
- [API 엔드포인트](#api-엔드포인트)
  - [평가 생성](#평가-생성)
  - [평가 수정](#평가-수정)
  - [평가 삭제](#평가-삭제)
  - [평가 조회](#평가-조회)
  - [평가 요약](#평가-요약)
- [데이터 모델](#데이터-모델)
- [비즈니스 규칙](#비즈니스-규칙)
- [오류 처리](#오류-처리)

## 개요

Evaluation API는 지원서에 대한 평가를 생성, 수정, 삭제, 조회하는 기능을 제공합니다. 각 평가자는 동일한 지원서에 대해 하나의 평가만 작성할 수 있으며, 평가 시 지원서의 평균 점수가 자동으로 계산됩니다.

**Base URL:** `/api/evaluations`

## 인증 및 권한

- **모든 엔드포인트**: Admin 권한 필요 (JWT 토큰 기반 인증)
- **평가 수정/삭제**: 해당 평가를 작성한 본인만 가능
- **점수 범위**: 0점 이상 100점 이하

## API 엔드포인트

### 평가 생성

#### POST /
지원서에 대한 새로운 평가를 생성합니다.

**요청 헤더**
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**요청 본문**
```json
{
  "applicationId": 1,
  "score": 85,
  "comment": "전반적으로 우수한 지원서입니다. 기술적 역량이 뛰어나고 프로젝트 경험이 풍부합니다."
}
```

**필드 설명**
- `applicationId` (Long, required): 평가할 지원서 ID
- `score` (Integer, required): 평가 점수 (0~100점)
- `comment` (String, optional): 평가 코멘트

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "applicationId": 1,
    "applicantName": "홍길동",
    "evaluatorId": 1,
    "evaluatorName": "평가자1",
    "score": 85,
    "comment": "전반적으로 우수한 지원서입니다.",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  "message": "'홍길동' 지원자에 대한 평가가 성공적으로 등록되었습니다. (점수: 85점)",
  "status": 200,
  "code": 0
}
```

**오류 응답**
- **400 Bad Request**: 유효성 검증 실패 (점수 범위 초과 등)
- **404 Not Found**: 지원서를 찾을 수 없음
- **409 Conflict**: 이미 해당 지원서에 평가를 작성함

### 평가 수정

#### PUT /{evaluationId}
기존 평가의 점수와 코멘트를 수정합니다.

**경로 매개변수**
- `evaluationId`: 수정할 평가의 ID (Long)

**요청 본문**
```json
{
  "score": 90,
  "comment": "재검토 결과 더 높은 점수를 주고 싶습니다."
}
```

**필드 설명**
- `score` (Integer, required): 수정할 점수 (0~100점)
- `comment` (String, optional): 수정할 코멘트

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "applicationId": 1,
    "applicantName": "홍길동",
    "evaluatorId": 1,
    "evaluatorName": "평가자1",
    "score": 90,
    "comment": "재검토 결과 더 높은 점수를 주고 싶습니다.",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:30:00"
  },
  "message": "'홍길동' 지원자에 대한 평가가 성공적으로 수정되었습니다. (변경된 점수: 90점)",
  "status": 200,
  "code": 0
}
```

**오류 응답**
- **400 Bad Request**: 유효성 검증 실패
- **403 Forbidden**: 수정 권한 없음 (본인이 작성하지 않은 평가)
- **404 Not Found**: 평가를 찾을 수 없음

### 평가 삭제

#### DELETE /{evaluationId}
기존 평가를 삭제합니다.

**경로 매개변수**
- `evaluationId`: 삭제할 평가의 ID (Long)

**성공 응답 (200)**
```json
{
  "success": true,
  "data": null,
  "message": "평가 ID 1이 성공적으로 삭제되었습니다.",
  "status": 200,
  "code": 0
}
```

**오류 응답**
- **403 Forbidden**: 삭제 권한 없음 (본인이 작성하지 않은 평가)
- **404 Not Found**: 평가를 찾을 수 없음

### 평가 조회

#### GET /{evaluationId}
특정 평가의 상세 정보를 조회합니다.

**경로 매개변수**
- `evaluationId`: 조회할 평가의 ID (Long)

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "applicationId": 1,
    "applicantName": "홍길동",
    "evaluatorId": 1,
    "evaluatorName": "평가자1",
    "score": 85,
    "comment": "전반적으로 우수한 지원서입니다.",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

#### GET /application/{applicationId}
특정 지원서에 대한 모든 평가를 조회합니다.

**경로 매개변수**
- `applicationId`: 지원서 ID (Long)

**성공 응답 (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "applicationId": 1,
      "applicantName": "홍길동",
      "evaluatorId": 1,
      "evaluatorName": "평가자1",
      "score": 85,
      "comment": "우수한 지원서입니다.",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    },
    {
      "id": 2,
      "applicationId": 1,
      "applicantName": "홍길동",
      "evaluatorId": 2,
      "evaluatorName": "평가자2",
      "score": 80,
      "comment": "괜찮은 수준입니다.",
      "createdAt": "2024-01-01T11:00:00",
      "updatedAt": "2024-01-01T11:00:00"
    }
  ]
}
```

#### GET /evaluator/my
현재 로그인한 평가자의 모든 평가를 조회합니다.

**성공 응답 (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "applicationId": 1,
      "applicantName": "홍길동",
      "evaluatorId": 1,
      "evaluatorName": "평가자1",
      "score": 85,
      "comment": "우수한 지원서입니다.",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    },
    {
      "id": 3,
      "applicationId": 2,
      "applicantName": "김철수",
      "evaluatorId": 1,
      "evaluatorName": "평가자1",
      "score": 75,
      "comment": "보통 수준입니다.",
      "createdAt": "2024-01-01T12:00:00",
      "updatedAt": "2024-01-01T12:00:00"
    }
  ]
}
```

### 평가 요약

#### GET /application/{applicationId}/summary
특정 지원서의 평가 요약 정보를 조회합니다.

**경로 매개변수**
- `applicationId`: 평가 요약을 조회할 지원서 ID (Long)

**성공 응답 (200)**
```json
{
  "success": true,
  "data": {
    "applicationId": 1,
    "applicantName": "홍길동",
    "averageScore": 82.5,
    "evaluationCount": 2,
    "evaluations": [
      {
        "id": 1,
        "applicationId": 1,
        "applicantName": "홍길동",
        "evaluatorId": 1,
        "evaluatorName": "평가자1",
        "score": 85,
        "comment": "우수한 지원서입니다.",
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:00:00"
      },
      {
        "id": 2,
        "applicationId": 1,
        "applicantName": "홍길동",
        "evaluatorId": 2,
        "evaluatorName": "평가자2",
        "score": 80,
        "comment": "괜찮은 수준입니다.",
        "createdAt": "2024-01-01T11:00:00",
        "updatedAt": "2024-01-01T11:00:00"
      }
    ]
  },
  "status": 200,
  "code": 0
}
```

**포함 정보**
- 지원자 기본 정보 (ID, 이름)
- 평균 점수 (모든 평가자의 점수 평균)
- 총 평가 개수
- 개별 평가 목록 (평가자, 점수, 코멘트 포함)

## 데이터 모델

### EvaluationRequest
```json
{
  "applicationId": "Long (required) - 평가할 지원서 ID",
  "score": "Integer (required, 0~100) - 평가 점수",
  "comment": "String (optional) - 평가 코멘트"
}
```

### EvaluationUpdateRequest
```json
{
  "score": "Integer (required, 0~100) - 수정할 점수",
  "comment": "String (optional) - 수정할 코멘트"
}
```

### EvaluationResponse
```json
{
  "id": "Long - 평가 ID",
  "applicationId": "Long - 지원서 ID",
  "applicantName": "String - 지원자 이름",
  "evaluatorId": "Long - 평가자 ID",
  "evaluatorName": "String - 평가자 이름",
  "score": "Integer - 평가 점수",
  "comment": "String - 평가 코멘트",
  "createdAt": "LocalDateTime - 평가 생성 시간",
  "updatedAt": "LocalDateTime - 평가 수정 시간"
}
```

### ApplicationEvaluationSummaryResponse
```json
{
  "applicationId": "Long - 지원서 ID",
  "applicantName": "String - 지원자 이름",
  "averageScore": "Double - 평균 점수",
  "evaluationCount": "Integer - 평가 개수",
  "evaluations": "List<EvaluationResponse> - 평가 목록"
}
```

## 비즈니스 규칙

### 평가 작성 규칙
1. **중복 평가 제한**: 한 평가자는 동일한 지원서에 대해 하나의 평가만 작성 가능
2. **점수 범위**: 0점 이상 100점 이하만 허용
3. **자동 계산**: 평가 생성/수정/삭제 시 해당 지원서의 평균 점수 자동 업데이트

### 권한 관리
1. **평가 작성**: 인증된 Admin 사용자만 가능
2. **평가 수정**: 해당 평가를 작성한 본인만 가능
3. **평가 삭제**: 해당 평가를 작성한 본인만 가능
4. **평가 조회**: 모든 Admin 사용자 가능

### 데이터 무결성
1. **지원서 존재 확인**: 평가 생성 시 해당 지원서가 존재하는지 확인
2. **평가자 정보**: JWT 토큰에서 평가자 정보 자동 추출
3. **타임스탬프**: 생성/수정 시간 자동 기록

## 오류 처리

### 오류 코드 및 메시지

| 상태 코드 | 오류 유형 | 설명 | 예시 메시지 |
|---------|---------|------|------------|
| 400 | Bad Request | 유효성 검증 실패 | "점수는 100점 이하여야 합니다. 현재 입력된 값: 150" |
| 401 | Unauthorized | 인증되지 않은 요청 | "인증이 필요합니다." |
| 403 | Forbidden | 권한 없음 | "평가자 '이영희'는 평가 ID 5에 대한 권한이 없습니다." |
| 404 | Not Found | 리소스를 찾을 수 없음 | "지원서 ID 999를 찾을 수 없습니다." |
| 409 | Conflict | 중복 평가 시도 | "평가자 '김철수'는 이미 지원서 ID 1에 대한 평가를 등록하셨습니다." |
| 500 | Internal Server Error | 서버 내부 오류 | "서버 오류가 발생했습니다." |

### 오류 응답 형식
```json
{
  "success": false,
  "data": null,
  "message": "구체적인 오류 메시지",
  "status": 400,
  "code": 1001,
  "time": "2024-01-01T10:00:00"
}
```

### 유효성 검증 오류 예시
```json
{
  "success": false,
  "data": null,
  "message": "score: 점수는 100점 이하여야 합니다. 현재 입력된 값: 150",
  "status": 400,
  "code": 1001,
  "time": "2024-01-01T10:00:00"
}
```

## 사용 예시

### 1. 새 평가 작성 플로우
```bash
# 1. 지원서 목록 조회 (applicationId 확인)
GET /api/webhook/applications

# 2. 평가 작성
POST /api/evaluations
{
  "applicationId": 1,
  "score": 85,
  "comment": "우수한 지원서입니다."
}

# 3. 작성한 평가 확인
GET /api/evaluations/evaluator/my
```

### 2. 지원서별 평가 현황 확인
```bash
# 1. 특정 지원서의 모든 평가 조회
GET /api/evaluations/application/1

# 2. 평가 요약 정보 조회
GET /api/evaluations/application/1/summary
```

### 3. 평가 수정 플로우
```bash
# 1. 내 평가 목록 확인
GET /api/evaluations/evaluator/my

# 2. 특정 평가 수정
PUT /api/evaluations/1
{
  "score": 90,
  "comment": "재검토 후 점수 상향 조정"
}
```
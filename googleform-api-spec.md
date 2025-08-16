# Google Form 도메인 API 명세서

## 개요
구글 폼을 관리하고 리쿠르팅 기간을 설정하는 API입니다. 구글 폼 생성, 조회, 활성화/비활성화, 통계 정보 조회 등의 기능을 제공합니다.

**Base URL**: `/api/google-forms`  
**태그**: GoogleForm  
**설명**: 구글 폼 관리 API

---

## 데이터 모델

### GoogleForm Entity
```java
{
  "id": "Long",
  "formId": "String (고유, 필수)",
  "title": "String (필수)",
  "formUrl": "String (필수, URL 형식)",
  "sheetUrl": "String (선택, URL 형식)",
  "isActive": "Boolean (기본값: false)",
  "description": "String (선택)",
  "recruitingStartDate": "LocalDateTime (선택)",
  "recruitingEndDate": "LocalDateTime (선택)",
  "generation": "Integer (필수, 1 이상)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### Request DTO (GoogleFormRequest)
```java
{
  "formId": "String (필수, 최대 100자)",
  "title": "String (필수, 2-100자)",
  "formUrl": "String (필수, URL 형식, 최대 1000자)",
  "sheetUrl": "String (선택, URL 형식, 최대 1000자)",
  "description": "String (선택, 최대 2000자)",
  "recruitingStartDate": "LocalDateTime (선택)",
  "recruitingEndDate": "LocalDateTime (선택)",
  "generation": "Integer (필수, 1 이상)"
}
```

### Response DTO (GoogleFormResponse)
```java
{
  "id": "Long",
  "formId": "String",
  "title": "String",
  "formUrl": "String",
  "sheetUrl": "String",
  "isActive": "Boolean",
  "description": "String",
  "generation": "Integer",
  "recruitingStartDate": "LocalDateTime",
  "recruitingEndDate": "LocalDateTime",
  "applicationCount": "Long (조건부 포함)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

---

## API 엔드포인트

### 1. 구글 폼 생성
**POST** `/api/google-forms`

**요청 본문**:
```json
{
  "formId": "1FAIpQLSd...",
  "title": "24기 리크루팅",
  "formUrl": "https://forms.google.com/...",
  "sheetUrl": "https://docs.google.com/spreadsheets/...",
  "description": "24기 피로그래밍 리크루팅 폼",
  "recruitingStartDate": "2024-01-15T09:00:00",
  "recruitingEndDate": "2024-01-30T23:59:59",
  "generation": 24
}
```

**응답**:
- **201 Created**: 구글 폼 생성 성공
- **400 Bad Request**: 유효성 검증 실패

---

### 2. 전체 구글 폼 조회
**GET** `/api/google-forms?includeApplicationCount=true`

**쿼리 파라미터**:
- `includeApplicationCount` (boolean, 기본값: true): 지원서 개수 포함 여부

**응답**:
- **200 OK**: 구글 폼 목록 (최신순 정렬)

---

### 3. 특정 구글 폼 조회 (ID 기준)
**GET** `/api/google-forms/{id}?includeApplicationCount=true`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**쿼리 파라미터**:
- `includeApplicationCount` (boolean, 기본값: true): 지원서 개수 포함 여부

**응답**:
- **200 OK**: 구글 폼 정보
- **404 Not Found**: 구글 폼을 찾을 수 없음

---

### 4. 폼 ID로 구글 폼 조회
**GET** `/api/google-forms/form-id/{formId}?includeApplicationCount=true`

**경로 파라미터**:
- `formId` (String): 구글 폼 ID

**응답**:
- **200 OK**: 구글 폼 정보
- **404 Not Found**: 구글 폼을 찾을 수 없음

---

### 5. 현재 활성화된 구글 폼 조회
**GET** `/api/google-forms/active`

**응답**:
- **200 OK**: 활성화된 구글 폼 정보 (지원서 개수 포함)
- **404 Not Found**: 활성화된 구글 폼이 없음

---

### 6. 구글 폼 활성화
**PUT** `/api/google-forms/{id}/activate`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**동작**:
- 지정된 구글 폼을 활성화
- 기존 활성화된 구글 폼은 자동으로 비활성화

**응답**:
- **200 OK**: 구글 폼 활성화 성공

---

### 7. 구글 폼 비활성화
**PUT** `/api/google-forms/{id}/deactivate`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**응답**:
- **200 OK**: 구글 폼 비활성화 성공

---

### 8. 구글 폼 URL 업데이트
**PUT** `/api/google-forms/{id}/form-url`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**요청 본문**:
```json
{
  "formUrl": "https://forms.google.com/new-url"
}
```

**응답**:
- **200 OK**: URL 업데이트 성공
- **400 Bad Request**: 잘못된 URL 형식

---

### 9. 구글 시트 URL 업데이트
**PUT** `/api/google-forms/{id}/sheet-url`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**요청 본문**:
```json
{
  "sheetUrl": "https://docs.google.com/spreadsheets/new-url"
}
```

**응답**:
- **200 OK**: URL 업데이트 성공
- **400 Bad Request**: 잘못된 URL 형식

---

### 10. 구글 폼 통계 정보 조회
**GET** `/api/google-forms/{id}/statistics`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**응답**:
```json
{
  "success": true,
  "data": {
    "googleFormId": 1,
    "formId": "1FAIpQLSd...",
    "formTitle": "24기 리크루팅",
    "totalApplications": 150,
    "statusStatistics": {
      "PENDING": 50,
      "IN_PROGRESS": 30,
      "COMPLETED": 70
    },
    "isActive": true,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

---

### 11. 제목으로 구글 폼 검색
**GET** `/api/google-forms/search?title={title}`

**쿼리 파라미터**:
- `title` (String): 검색할 제목

**응답**:
- **200 OK**: 검색된 구글 폼 목록

---

### 12. 구글 폼 삭제
**DELETE** `/api/google-forms/{id}`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**제약사항**:
- 활성화된 구글 폼은 삭제할 수 없음

**응답**:
- **200 OK**: 구글 폼 삭제 성공
- **400 Bad Request**: 활성화된 구글 폼 삭제 시도

---

### 13. 특정 기수의 구글 폼들 조회
**GET** `/api/google-forms/generation/{generation}?includeApplicationCount=true`

**경로 파라미터**:
- `generation` (Integer): 기수

**쿼리 파라미터**:
- `includeApplicationCount` (boolean, 기본값: true): 지원서 개수 포함 여부

**응답**:
- **200 OK**: 해당 기수의 구글 폼 목록

---

### 14. 특정 기수의 활성화된 구글 폼 조회
**GET** `/api/google-forms/generation/{generation}/active`

**경로 파라미터**:
- `generation` (Integer): 기수

**응답**:
- **200 OK**: 해당 기수의 활성화된 구글 폼 정보
- **404 Not Found**: 해당 기수에 활성화된 구글 폼이 없음

---

### 15. 현재 활성화된 기수 조회
**GET** `/api/google-forms/current-generation`

**응답**:
```json
{
  "success": true,
  "data": {
    "currentGeneration": 24,
    "message": "현재 24기가 활성화되어 있습니다."
  }
}
```

**상태 코드**:
- **200 OK**: 활성화된 기수 정보
- **404 Not Found**: 활성화된 구글 폼이 없음

---

### 16. 가장 최신 기수 조회
**GET** `/api/google-forms/latest-generation`

**응답**:
```json
{
  "success": true,
  "data": {
    "latestGeneration": 25,
    "message": "가장 최신 기수는 25기입니다."
  }
}
```

**상태 코드**:
- **200 OK**: 최신 기수 정보
- **404 Not Found**: 등록된 구글 폼이 없음

---

### 17. 구글 폼 기수 업데이트
**PUT** `/api/google-forms/{id}/generation`

**경로 파라미터**:
- `id` (Long): 구글 폼 ID

**요청 본문**:
```json
{
  "generation": 25
}
```

**응답**:
- **200 OK**: 기수 업데이트 성공
- **400 Bad Request**: 잘못된 기수 값 (1 이상이어야 함)

---

### 18. 기수 범위로 구글 폼 조회
**GET** `/api/google-forms/generation-range?startGeneration={start}&endGeneration={end}`

**쿼리 파라미터**:
- `startGeneration` (Integer): 시작 기수
- `endGeneration` (Integer): 끝 기수

**응답**:
- **200 OK**: 해당 기수 범위의 구글 폼 목록
- **400 Bad Request**: 잘못된 기수 범위 (시작 기수 > 끝 기수)

---

## 공통 응답 형식

모든 API는 `ApiRes<T>` 래퍼를 사용합니다:

### 성공 응답
```json
{
  "success": true,
  "data": {...},
  "message": "성공 메시지",
  "status": 200,
  "code": "SUCCESS",
  "time": "2024-01-15T10:30:00"
}
```

### 실패 응답
```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지",
  "status": 400,
  "code": "ERROR_CODE",
  "time": "2024-01-15T10:30:00"
}
```

---

## 주요 비즈니스 로직

### 1. 구글 폼 활성화 규칙
- 한 번에 하나의 구글 폼만 활성화 가능
- 새로운 구글 폼을 활성화하면 기존 활성화된 구글 폼은 자동으로 비활성화

### 2. 지원서 개수 조회 최적화
- N+1 문제 해결을 위해 배치 쿼리 사용
- `includeApplicationCount` 파라미터로 선택적 포함 가능

### 3. 유효성 검증
- URL 형식 검증 (http:// 또는 https://로 시작)
- 기수는 1 이상의 양수만 허용
- 필수 필드 검증 (formId, title, formUrl, generation)

### 4. 삭제 제약사항
- 활성화된 구글 폼은 삭제 불가
- 삭제 전 비활성화 필요

---

## 인증 및 권한

- 모든 API는 JWT 인증이 필요합니다
- 관리자 권한이 필요한 API입니다
- CORS가 모든 origin에 대해 활성화되어 있습니다

---

## 에러 코드

| 코드 | 설명 |
|------|------|
| `NOT_FOUND` | 리소스를 찾을 수 없음 |
| `INVALID_ARGUMENT` | 잘못된 파라미터 |
| `GOOGLE_FORM_NOT_ACTIVE` | 활성화된 구글 폼이 없음 |

---

## 사용 예시

### 새로운 리크루팅 시작하기
1. 구글 폼 생성: `POST /api/google-forms`
2. 구글 폼 활성화: `PUT /api/google-forms/{id}/activate`
3. 현재 활성화된 구글 폼 확인: `GET /api/google-forms/active`

### 지원서 현황 모니터링
1. 활성화된 구글 폼 조회: `GET /api/google-forms/active`
2. 통계 정보 조회: `GET /api/google-forms/{id}/statistics`
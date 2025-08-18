# Admin API 명세서

## 개요
관리자 인증, 권한 관리, 지원서 관리를 위한 Admin API 명세서입니다.

## Base URL
```
http://localhost:8080/api/admin
```

## 인증 방식
- JWT 기반 Bearer Token 인증
- Access Token과 Refresh Token을 사용한 토큰 로테이션 방식

---

## 1. 인증 관리 API

### 1.1 로그인
관리자 로그인을 수행합니다.

**Endpoint:** `POST /login`

**Request Body:**
```json
{
  "loginCode": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

**Status Codes:**
- `200 OK`: 로그인 성공
- `401 Unauthorized`: 잘못된 로그인 코드
- `403 Forbidden`: 만료된 관리자 계정

---

### 1.2 토큰 갱신
Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

**Endpoint:** `POST /refresh`

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

**Status Codes:**
- `200 OK`: 토큰 갱신 성공
- `401 Unauthorized`: 유효하지 않은 Refresh Token

---

### 1.3 로그아웃
Refresh Token을 무효화하여 로그아웃을 수행합니다.

**Endpoint:** `POST /logout`

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response:**
- Status: `200 OK`
- Body: 없음

**Status Codes:**
- `200 OK`: 로그아웃 성공
- `401 Unauthorized`: 유효하지 않은 Refresh Token

---

### 1.4 API Key 토큰 교환
외부 서비스에서 API Key를 JWT 토큰으로 교환합니다.

**Endpoint:** `POST /token/exchange`

**Request Body:**
```json
{
  "apiKey": "string",
  "purpose": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "purpose": "string"
}
```

**Status Codes:**
- `200 OK`: 토큰 교환 성공
- `401 Unauthorized`: 유효하지 않은 API Key

---

## 2. General Admin 관리 API
**권한 필요:** ROOT 권한

### 2.1 General Admin 생성
새로운 일반 관리자를 생성합니다.

**Endpoint:** `POST /general`

**Request Body:**
```json
{
  "identifierName": "string",
  "expiredAt": "2024-12-31T23:59:59"
}
```

**Response:**
```json
{
  "id": 1,
  "loginCode": "ABC12345",
  "identifierName": "평가자-001",
  "role": "GENERAL",
  "expiredAt": "2024-12-31T23:59:59",
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

**Status Codes:**
- `200 OK`: 생성 성공
- `403 Forbidden`: 권한 없음

---

### 2.2 General Admin 일괄 생성
여러 일반 관리자를 일괄 생성합니다.

**Endpoint:** `POST /general/batch`

**Request Body:**
```json
{
  "count": 5,
  "expiredAt": "2024-12-31T23:59:59"
}
```

**Response:**
```json
{
  "count": 5,
  "admins": [
    {
      "id": 1,
      "loginCode": "ABC12345",
      "identifierName": "평가자-001",
      "role": "GENERAL",
      "expiredAt": "2024-12-31T23:59:59",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

**Status Codes:**
- `200 OK`: 생성 성공
- `403 Forbidden`: 권한 없음

---

### 2.3 General Admin 목록 조회
모든 일반 관리자 목록을 조회합니다.

**Endpoint:** `GET /general`

**Response:**
```json
[
  {
    "id": 1,
    "loginCode": "ABC12345",
    "identifierName": "평가자-001",
    "role": "GENERAL",
    "expiredAt": "2024-12-31T23:59:59",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
]
```

**Status Codes:**
- `200 OK`: 조회 성공
- `403 Forbidden`: 권한 없음

---

### 2.4 만료된 General Admin 삭제
만료된 일반 관리자를 삭제합니다.

**Endpoint:** `DELETE /general/expired`

**Response:**
- Status: `200 OK`
- Body: 없음

**Status Codes:**
- `200 OK`: 삭제 성공
- `403 Forbidden`: 권한 없음

---

### 2.5 모든 General Admin 삭제
모든 일반 관리자를 삭제합니다.

**Endpoint:** `DELETE /general/all`

**Response:**
- Status: `200 OK`
- Body: 없음

**Status Codes:**
- `200 OK`: 삭제 성공
- `403 Forbidden`: 권한 없음

---

## 3. 지원서 관리 API
**Base URL:** `/api/admin/applications`
**권한 필요:** ROOT 또는 GENERAL 권한

### 3.1 개별 합격 상태 변경
특정 지원자의 합격 상태를 변경합니다.

**Endpoint:** `PUT /applications/{id}/pass-status`

**Path Parameters:**
- `id`: 지원서 ID (Long)

**Request Body:**
```json
{
  "passStatus": "PASS" // PASS, FAIL, PENDING
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "googleFormId": 1,
    "submissionId": "submission123",
    "passStatus": "PASS",
    "responses": {},
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "message": "합격 상태가 변경되었습니다.",
  "status": 200
}
```

---

### 3.2 일괄 합격 상태 변경
여러 지원자의 합격 상태를 일괄 변경합니다.

**Endpoint:** `PUT /applications/all/pass-status`

**Request Body:**
```json
{
  "applicationIds": [1, 2, 3],
  "passStatus": "PASS"
}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "googleFormId": 1,
      "submissionId": "submission123",
      "passStatus": "PASS",
      "responses": {},
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "message": "3명의 합격 상태가 변경되었습니다.",
  "status": 200
}
```

---

### 3.3 합격 상태별 지원서 조회
특정 합격 상태의 지원자들을 조회합니다.

**Endpoint:** `GET /applications/pass-status/{status}`

**Path Parameters:**
- `status`: 합격 상태 (PASS, FAIL, PENDING)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "googleFormId": 1,
      "submissionId": "submission123",
      "passStatus": "PASS",
      "responses": {},
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "message": "PASS 상태의 지원자 5명을 조회했습니다.",
  "status": 200
}
```

---

### 3.4 합격 상태 통계 조회
전체 지원자의 합격 상태 통계를 조회합니다.

**Endpoint:** `GET /applications/pass-status/statistics`

**Response:**
```json
{
  "success": true,
  "data": {
    "PASS": 10,
    "FAIL": 5,
    "PENDING": 15
  },
  "message": "합격 상태 통계를 조회했습니다.",
  "status": 200
}
```

---

### 3.5 구글 폼별 합격 상태 통계 조회
특정 구글 폼의 합격 상태 통계를 조회합니다.

**Endpoint:** `GET /applications/google-form/{googleFormId}/pass-status/statistics`

**Path Parameters:**
- `googleFormId`: 구글 폼 ID (Long)

**Response:**
```json
{
  "success": true,
  "data": {
    "PASS": 5,
    "FAIL": 2,
    "PENDING": 8
  },
  "message": "구글 폼 1의 합격 상태 통계를 조회했습니다.",
  "status": 200
}
```

---

### 3.6 구글 폼별 + 합격 상태별 지원서 조회
특정 구글 폼의 특정 합격 상태 지원자들을 조회합니다.

**Endpoint:** `GET /applications/google-form/{googleFormId}/pass-status/{status}`

**Path Parameters:**
- `googleFormId`: 구글 폼 ID (Long)
- `status`: 합격 상태 (PASS, FAIL, PENDING)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "googleFormId": 1,
      "submissionId": "submission123",
      "passStatus": "PASS",
      "responses": {},
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "message": "구글 폼 1의 PASS 상태 지원자 3명을 조회했습니다.",
  "status": 200
}
```

---

## 4. 에러 코드

### 4.1 인증 관련 에러
- `INVALID_LOGIN_CODE`: 잘못된 로그인 코드
- `EXPIRED_ADMIN`: 만료된 관리자 계정
- `INVALID_REFRESH_TOKEN`: 유효하지 않은 Refresh Token
- `INVALID_API_KEY`: 유효하지 않은 API Key

### 4.2 권한 관련 에러
- `ADMIN_NOT_FOUND`: 관리자를 찾을 수 없음
- `ACCESS_DENIED`: 접근 권한 없음

---

## 5. 권한 체계

### 5.1 Admin Role
- `ROOT`: 모든 기능 접근 가능
- `GENERAL`: 지원서 관리 기능만 접근 가능
- `WEBHOOK`: 웹훅 전용 (내부 시스템용)

### 5.2 권한별 접근 가능 API
- **ROOT**: 모든 API 접근 가능
- **GENERAL**: 지원서 관리 API만 접근 가능
- **WEBHOOK**: 웹훅 관련 API만 접근 가능

---

## 6. 보안 고려사항

### 6.1 토큰 관리
- Access Token 만료시간: 1시간
- Refresh Token 만료시간: 7일
- 토큰 로테이션 방식으로 보안성 강화

### 6.2 General Admin 관리
- 만료 날짜 설정으로 임시 계정 관리
- 로그인 코드는 UUID 기반 8자리 자동 생성
- 만료된 계정 자동 삭제 기능

### 6.3 API Key 관리
- 환경변수로 관리되는 웹훅 전용 API Key
- 외부 서비스 인증용도로만 사용
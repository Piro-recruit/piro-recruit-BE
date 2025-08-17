# Integration 도메인 API 명세서

## 개요
외부 시스템 연동 및 데이터 내보내기를 위한 API입니다. 주로 Apps Script 연동과 CSV 파일 내보내기 기능을 제공합니다.

**Base URL**: `/api/integration`  
**태그**: Integration  
**설명**: 외부 시스템 연동 API (Apps Script, CSV 내보내기)  
**권한**: ROOT 또는 GENERAL 권한 필요

---

## 데이터 모델

### AppsScriptDto

#### ExportRequest
```java
{
  "googleFormId": "Long (선택)",
  "exportType": "String (applicants, admins, all)",
  "options": "Map<String, Object> (추가 옵션)"
}
```

#### ExportResponse
```java
{
  "downloadUrl": "String",
  "filename": "String", 
  "recordCount": "long",
  "generatedAt": "LocalDateTime",
  "status": "String (success, failed)",
  "message": "String"
}
```

#### ConnectionTestResponse
```java
{
  "connected": "boolean",
  "message": "String",
  "testedAt": "LocalDateTime",
  "version": "String (Apps Script 버전)"
}
```

#### PreviewRequest
```java
{
  "googleFormId": "Long (선택)",
  "dataType": "String (applicants, admins)",
  "limit": "int"
}
```

#### StatisticsResponse
```java
{
  "googleFormId": "Long (선택)",
  "totalApplicantCount": "long",
  "totalAdminCount": "long",
  "activeAdminCount": "long",
  "currentRecruitmentLevel": "int",
  "lastUpdated": "LocalDateTime",
  "additionalStats": "Map<String, Object>"
}
```

---

## API 엔드포인트

### 1. 지원자 CSV 내보내기
**GET** `/api/integration/export/applicants/csv?googleFormId={googleFormId}`

**쿼리 파라미터**:
- `googleFormId` (Long, 선택): 구글 폼 ID (없으면 전체 지원자)

**응답**:
- **Content-Type**: `text/csv; charset=UTF-8`
- **Content-Disposition**: `attachment; filename="applicants_form_{id}.csv"` 또는 `"applicants_all.csv"`

**CSV 형식**:
```csv
name,phone,level,major,is_passed
김피로,010-1234-5678,24,1,1
이개발,010-5678-1234,24,0,0
```

**필드 설명**:
- `name`: 지원자 이름
- `phone`: 전화번호 (010-1234-5678 형식)
- `level`: 기수 (구글 폼의 generation)
- `major`: 전공 여부 (0:비전공, 1:전공, 2:복수전공)
- `is_passed`: 합격 상태 (0:대기/불합격, 1:1차합격, 2:최종합격)

---

### 2. Admin 코드 CSV 내보내기
**GET** `/api/integration/export/admins/csv`

**응답**:
- **Content-Type**: `text/csv; charset=UTF-8`
- **Content-Disposition**: `attachment; filename="admin_codes_{timestamp}.csv"`

**CSV 형식**:
```csv
loginCode,identifierName,expiredAt
ADMIN001,피로관리자,2024-12-31 23:59:59
ADMIN002,개발관리자,2024-12-31 23:59:59
```

**필드 설명**:
- `loginCode`: 관리자 로그인 코드
- `identifierName`: 관리자 식별 이름
- `expiredAt`: 만료 일시 (yyyy-MM-dd HH:mm:ss 형식)

---

### 3. 지원자 CSV 미리보기
**GET** `/api/integration/preview/applicants?googleFormId={googleFormId}&limit={limit}`

**쿼리 파라미터**:
- `googleFormId` (Long, 선택): 구글 폼 ID
- `limit` (int, 기본값: 10): 미리보기 행 수

**응답**:
```json
{
  "success": true,
  "data": "name,phone,level,major,is_passed\n김피로,010-1234-5678,24,1,1\n...",
  "message": "지원자 CSV 미리보기가 생성되었습니다.",
  "status": 200,
  "code": "SUCCESS",
  "time": "2024-08-16T10:30:00"
}
```

---

### 4. Admin 코드 CSV 미리보기
**GET** `/api/integration/preview/admins?limit={limit}`

**쿼리 파라미터**:
- `limit` (int, 기본값: 10): 미리보기 행 수

**응답**:
```json
{
  "success": true,
  "data": "loginCode,identifierName,expiredAt\nADMIN001,피로관리자,2024-12-31 23:59:59\n...",
  "message": "Admin 코드 CSV 미리보기가 생성되었습니다.",
  "status": 200,
  "code": "SUCCESS", 
  "time": "2024-08-16T10:30:00"
}
```

---

### 5. CSV 내보내기 통계
**GET** `/api/integration/export/statistics?googleFormId={googleFormId}`

**쿼리 파라미터**:
- `googleFormId` (Long, 선택): 구글 폼 ID

**응답 (특정 구글 폼)**:
```json
{
  "success": true,
  "data": {
    "googleFormId": 1,
    "applicantCount": 150,
    "recruitmentLevel": 24,
    "passStatusStatistics": {
      "PENDING": 50,
      "FAILED": 30,
      "FIRST_PASS": 40,
      "FINAL_PASS": 30
    },
    "totalAdminCount": 10,
    "activeAdminCount": 8,
    "exportTimestamp": "2024-08-16T10:30:00"
  },
  "message": "CSV 내보내기 통계 정보입니다."
}
```

**응답 (전체 통계)**:
```json
{
  "success": true,
  "data": {
    "totalApplicantCount": 500,
    "generationStatistics": {
      "23": 200,
      "24": 300
    },
    "passStatusStatistics": {
      "PENDING": 100,
      "FAILED": 150,
      "FIRST_PASS": 150,
      "FINAL_PASS": 100
    },
    "totalAdminCount": 15,
    "activeAdminCount": 12,
    "exportTimestamp": "2024-08-16T10:30:00"
  },
  "message": "CSV 내보내기 통계 정보입니다."
}
```

---

### 6. Apps Script 연결 테스트
**GET** `/api/integration/test/connection`

**응답 (연결 성공)**:
```json
{
  "success": true,
  "data": "연결 성공",
  "message": "Apps Script와의 연결이 정상입니다.",
  "status": 200,
  "code": "SUCCESS",
  "time": "2024-08-16T10:30:00"
}
```

**응답 (연결 실패)**:
```json
{
  "success": true,
  "data": "연결 실패",
  "message": "Apps Script 연결에 문제가 있습니다.",
  "status": 200,
  "code": "SUCCESS",
  "time": "2024-08-16T10:30:00"
}
```

---

## 주요 비즈니스 로직

### 1. CSV 생성 로직

#### 지원자 CSV 생성
1. **데이터 조회**: googleFormId가 있으면 해당 폼의 지원자만, 없으면 전체 지원자 조회
2. **홈페이지 User ID 할당**: 없는 경우 자동으로 순차적 ID 할당
3. **전화번호 추출**: formData에서 다양한 키로 전화번호 검색
   - 검색 키: "전화번호", "연락처", "휴대폰번호", "phone", "mobile", "핸드폰", "휴대폰", "연락처 번호"
   - 자동 형식 변환: 010-1234-5678 형식으로 정리
4. **전공 여부 판단**: formData 또는 개별 필드에서 전공 정보 추출
   - 0: 비전공자
   - 1: IT 관련 전공자 (컴퓨터, 소프트웨어, 정보, 전산, AI 등)
   - 2: 복수전공자
5. **합격 상태 매핑**: PassStatus enum을 CSV 값으로 변환

#### Admin CSV 생성
1. **전체 일반 관리자 조회**: AdminService를 통해 GeneralAdmin 목록 조회
2. **만료일 형식 변환**: LocalDateTime을 "yyyy-MM-dd HH:mm:ss" 형식으로 변환
3. **CSV 안전성**: 특수문자 이스케이프 처리

### 2. 데이터 추출 및 변환

#### 전화번호 형식 정리
- 숫자만 추출 후 010-XXXX-XXXX 형식으로 변환
- 기존에 하이픈이 있는 경우 그대로 유지

#### CSV 이스케이프 처리
- 쉼표, 따옴표, 개행문자 포함 시 따옴표로 감싸기
- 따옴표는 두 개로 이스케이프 ("" 형태)

### 3. 통계 정보 생성
- **합격 상태별 통계**: PassStatus enum별 지원자 수 집계
- **기수별 통계**: generation별 지원자 수 집계  
- **관리자 통계**: 전체/활성 관리자 수 (만료일 기준)

---

## 인증 및 권한

- **필수 권한**: ROOT 또는 GENERAL 역할
- **보안 설정**: `@PreAuthorize("hasRole('ROOT') or hasRole('GENERAL')")`
- **JWT 인증**: 모든 엔드포인트에서 JWT 토큰 필요

---

## 파일 다운로드 헤더

### CSV 파일 다운로드
- **Content-Type**: `text/csv; charset=UTF-8`
- **Content-Disposition**: `attachment; filename="{filename}"`
- **파일명 규칙**:
  - 지원자 (특정 폼): `applicants_form_{googleFormId}.csv`
  - 지원자 (전체): `applicants_all.csv`
  - 관리자: `admin_codes_{yyyyMMdd_HHmmss}.csv`

---

## 에러 처리

- **권한 없음**: 403 Forbidden
- **데이터 없음**: 빈 CSV 반환 (헤더만 포함)
- **서버 오류**: 500 Internal Server Error

---

## 사용 예시

### 특정 기수 지원자 CSV 다운로드
```bash
GET /api/integration/export/applicants/csv?googleFormId=1
Authorization: Bearer {jwt_token}
```

### 전체 지원자 통계 조회
```bash
GET /api/integration/export/statistics
Authorization: Bearer {jwt_token}
```

### 지원자 데이터 미리보기 (상위 5개)
```bash
GET /api/integration/preview/applicants?limit=5
Authorization: Bearer {jwt_token}
```

---

## 외부 연동

### Apps Script 연동
- 현재는 연결 테스트만 구현
- 실제 구현 시 Google Apps Script API 호출
- CSV 데이터를 구글 시트로 자동 업로드 기능 예정

### 홈페이지 연동
- CSV 형식이 홈페이지 import 형식과 호환
- 자동으로 홈페이지 User ID 할당
- 합격자 정보 동기화